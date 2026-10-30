package com.linjian.seckill.domain.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.linjian.seckill.common.BizException;
import com.linjian.seckill.common.ErrorCode;
import com.linjian.seckill.common.PageResult;
import com.linjian.seckill.domain.dto.OrderVO;
import com.linjian.seckill.domain.entity.SkActivityGoods;
import com.linjian.seckill.domain.entity.SkGoods;
import com.linjian.seckill.domain.entity.SkIdempotentRecord;
import com.linjian.seckill.domain.entity.SkSeckillOrder;
import com.linjian.seckill.domain.enums.OrderStatus;
import com.linjian.seckill.domain.mapper.SkActivityGoodsMapper;
import com.linjian.seckill.domain.mapper.SkGoodsMapper;
import com.linjian.seckill.domain.mapper.SkIdempotentRecordMapper;
import com.linjian.seckill.domain.mapper.SkSeckillOrderMapper;
import com.linjian.seckill.domain.mq.SeckillOrderMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 异步落单。
 * <p>
 * 事务边界刻意切成两段：
 * <ol>
 *   <li>幂等记录插入（独立提交）——失败即代表这条消息处理过或正在处理，直接跳过；</li>
 *   <li>DB 扣库存 + 插订单（同一事务）——两者必须同生共死，否则库存扣了没订单，
 *       或者订单落了库存没扣。</li>
 * </ol>
 * 幂等记录不能放进第二个事务：那一次回滚会把「已处理」的标记一起撤掉，
 * 消息重投时会再处理一遍，补偿逻辑会跟着重复执行。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private static final String BIZ_TYPE = "SECKILL_ORDER";

    private final SkSeckillOrderMapper orderMapper;
    private final SkActivityGoodsMapper activityGoodsMapper;
    private final SkGoodsMapper goodsMapper;
    private final SkIdempotentRecordMapper idempotentMapper;
    private final SeckillResultCache resultCache;
    private final StockRedisService stockRedisService;
    private final TransactionTemplate transactionTemplate;

    @Value("${seckill.order.timeout-minutes:15}")
    private int timeoutMinutes;

    /**
     * 消费一条下单消息。
     * <p>
     * 业务性失败（重复购买、DB 库存不足）不回滚幂等记录，也不再重试：
     * 重试只会重复回补 Redis。系统异常（数据库不可用等）才回滚幂等记录并
     * 抛出去让 MQ 重投。
     */
    public void handleMessage(SeckillOrderMessage message) {
        SkIdempotentRecord idempotent = tryMarkProcessed(message);
        if (idempotent == null) {
            log.info("重复消息，跳过 messageId={} requestId={}", message.getMessageId(), message.getRequestId());
            return;
        }

        SkSeckillOrder order;
        try {
            order = transactionTemplate.execute(status -> doCreate(message));
        } catch (DuplicateBuyException e) {
            compensate(message, ErrorCode.DUPLICATE_BUY);
            return;
        } catch (InsufficientStockException e) {
            compensate(message, ErrorCode.STOCK_EMPTY);
            return;
        } catch (RuntimeException e) {
            rollbackIdempotent(idempotent);
            throw e;
        }

        // 结果写 Redis 必须在事务提交之后：写在事务里的话，一旦提交阶段失败，
        // 用户会看到「抢购成功」但订单根本不存在
        resultCache.markSuccess(message.getRequestId(), order.getId(), order.getOrderNo());
        log.info("落单成功 requestId={} orderNo={} userId={} goodsId={}",
                message.getRequestId(), order.getOrderNo(), message.getUserId(), message.getGoodsId());
    }

    // ------------------------------------------------------------
    // 事务内动作
    // ------------------------------------------------------------

    private SkSeckillOrder doCreate(SeckillOrderMessage message) {
        int quantity = message.getQuantity() == null ? 1 : message.getQuantity();

        SkActivityGoods relation = activityGoodsMapper.selectById(message.getActivityGoodsId());
        if (relation == null) {
            throw new InsufficientStockException("活动商品不存在 id=" + message.getActivityGoodsId());
        }
        deductDbStock(relation.getId(), quantity);

        SkGoods goods = goodsMapper.selectById(relation.getGoodsId());

        SkSeckillOrder order = new SkSeckillOrder();
        order.setOrderNo(generateOrderNo(message.getRequestId()));
        order.setRequestId(message.getRequestId());
        order.setUserId(message.getUserId());
        order.setActivityId(message.getActivityId());
        order.setActivityGoodsId(relation.getId());
        order.setGoodsId(relation.getGoodsId());
        // 商品名与价格从库里读，不信任消息体：消息可能被延迟投递，
        // 期间活动改价了也不能用旧价成交
        order.setGoodsName(goods == null ? "秒杀商品" : goods.getName());
        order.setPrice(relation.getSeckillPrice());
        order.setQuantity(quantity);
        order.setStatus(OrderStatus.PENDING.getCode());
        order.setExpireTime(LocalDateTime.now().plusMinutes(timeoutMinutes));

        try {
            orderMapper.insert(order);
        } catch (DuplicateKeyException e) {
            // 命中 uk_user_activity_goods：Redis 的用户防重被绕过
            // （例如活动进行中 Redis 被清过），这里兜住「一人多单」
            throw new DuplicateBuyException();
        }
        return order;
    }

    /**
     * DB 侧库存扣减。
     * <p>
     * 为什么还需要这一层，Redis 不是已经扣过了吗：Redis 的库存可能在
     * 服务重启、键过期、人工清理后与 DB 不一致。DB 是最终事实来源，
     * 它说没库存就是没库存，宁可少卖也不能超卖。
     * <p>
     * 只有一次尝试，没有重试循环：{@code WHERE stock >= n} 让这条 UPDATE
     * 天然串行化，影响行数为 0 只可能是真的不够卖。早期用「版本号 + 重试 3 次」
     * 的写法在 20 并发下会把 19 个请求全部打回重试并最终失败，是在给自己造锁。
     */
    private void deductDbStock(Long activityGoodsId, int quantity) {
        if (activityGoodsMapper.deductStock(activityGoodsId, quantity) == 0) {
            SkActivityGoods current = activityGoodsMapper.selectById(activityGoodsId);
            if (current == null) {
                throw new InsufficientStockException("活动商品已删除 id=" + activityGoodsId);
            }
            throw new InsufficientStockException("DB 库存不足 remain=" + current.getStock());
        }
    }

    // ------------------------------------------------------------
    // 幂等与补偿
    // ------------------------------------------------------------

    private SkIdempotentRecord tryMarkProcessed(SeckillOrderMessage message) {
        SkIdempotentRecord record = new SkIdempotentRecord();
        record.setMessageId(message.getMessageId());
        record.setBizType(BIZ_TYPE);
        record.setBizId(String.valueOf(message.getRequestId()));
        try {
            idempotentMapper.insert(record);
            return record;
        } catch (DuplicateKeyException e) {
            return null;
        }
    }

    private void rollbackIdempotent(SkIdempotentRecord record) {
        if (record != null && record.getId() != null) {
            idempotentMapper.deleteById(record.getId());
        }
    }

    /**
     * 业务失败后的补偿：把 Redis 里预扣的库存与已购计数还回去，并让前端
     * 轮询能拿到明确的失败原因，而不是一直 PENDING。
     */
    private void compensate(SeckillOrderMessage message, ErrorCode errorCode) {
        stockRedisService.restore(message.getActivityId(), message.getGoodsId(),
                message.getUserId(), message.getQuantity() == null ? 1 : message.getQuantity());
        resultCache.markFailed(message.getRequestId(), errorCode.getCode(), errorCode.getMessage());
        log.warn("落单失败已回补 requestId={} userId={} reason={}",
                message.getRequestId(), message.getUserId(), errorCode.getMessage());
    }

    // ------------------------------------------------------------
    // 查询
    // ------------------------------------------------------------

    public SkSeckillOrder findByRequestId(Long requestId) {
        return orderMapper.selectOne(Wrappers.<SkSeckillOrder>lambdaQuery()
                .eq(SkSeckillOrder::getRequestId, requestId));
    }

    /**
     * 我的订单。
     */
    public PageResult<OrderVO> pageByUser(Long userId, long pageNum, long pageSize) {
        Page<SkSeckillOrder> page = new Page<>(pageNum, pageSize);
        IPage<SkSeckillOrder> result = orderMapper.selectPage(page,
                Wrappers.<SkSeckillOrder>lambdaQuery()
                        .eq(SkSeckillOrder::getUserId, userId)
                        .orderByDesc(SkSeckillOrder::getCreateTime));
        return PageResult.of(
                result.getRecords().stream().map(this::toVO).toList(),
                result.getTotal(), result.getCurrent(), result.getSize());
    }

    /**
     * 模拟支付。
     * <p>
     * 真实项目这里要对接支付网关的回调，且必须校验签名与金额。这里的作用是把
     * 订单状态机跑通，让「超时关单」有一个对手方：已支付的订单不能被关掉。
     */
    public OrderVO pay(String orderNo, Long userId) {
        SkSeckillOrder order = orderMapper.selectOne(Wrappers.<SkSeckillOrder>lambdaQuery()
                .eq(SkSeckillOrder::getOrderNo, orderNo));
        if (order == null) {
            throw new BizException(ErrorCode.ORDER_NOT_FOUND);
        }
        if (!order.getUserId().equals(userId)) {
            throw new BizException(ErrorCode.FORBIDDEN);
        }
        // 条件更新：与关单任务竞争时只有一方能成功，避免「已关闭的订单被支付」
        if (orderMapper.payIfPending(order.getId(), LocalDateTime.now()) == 0) {
            throw new BizException(ErrorCode.ORDER_STATUS_INVALID);
        }
        log.info("订单已支付 orderNo={} userId={}", orderNo, userId);
        return toVO(orderMapper.selectById(order.getId()));
    }

    private OrderVO toVO(SkSeckillOrder order) {
        OrderVO vo = new OrderVO();
        vo.setId(order.getId());
        vo.setOrderNo(order.getOrderNo());
        vo.setGoodsName(order.getGoodsName());
        vo.setPrice(order.getPrice());
        vo.setQuantity(order.getQuantity());
        vo.setStatus(order.getStatus());
        OrderStatus status = OrderStatus.of(order.getStatus());
        vo.setStatusDesc(status == null ? "未知" : status.getDesc());
        vo.setExpireTime(order.getExpireTime());
        vo.setCreateTime(order.getCreateTime());
        vo.setPayTime(order.getPayTime());
        return vo;
    }

    /**
     * 查某用户在某活动商品上的订单。命中 uk_user_activity_goods 说明最多一条。
     */
    public SkSeckillOrder findByUserAndGoods(Long activityId, Long goodsId, Long userId) {
        return orderMapper.selectOne(Wrappers.<SkSeckillOrder>lambdaQuery()
                .eq(SkSeckillOrder::getActivityId, activityId)
                .eq(SkSeckillOrder::getGoodsId, goodsId)
                .eq(SkSeckillOrder::getUserId, userId));
    }

    // ------------------------------------------------------------
    // 关单（供定时任务调用）
    // ------------------------------------------------------------

    /**
     * 关掉一笔超时未支付的订单，并回补 DB 库存。
     * <p>
     * 用 {@code cancelIfPending} 做抢占：并发两次关单只有一次返回 1，
     * 回补动作因此不会重复执行。DB 回补与状态推进放在同一事务里，
     * 避免「库存还了但状态没改」导致下一轮再还一次。
     *
     * @return true 表示本次真的关掉了一笔订单
     */
    public boolean closeIfTimeout(Long orderId, LocalDateTime now) {
        SkSeckillOrder order = orderMapper.selectById(orderId);
        if (order == null) {
            return false;
        }

        Boolean closed = transactionTemplate.execute(status -> {
            if (orderMapper.cancelIfPending(orderId, now) == 0) {
                return false;
            }
            activityGoodsMapper.restoreStock(order.getActivityGoodsId(), order.getQuantity());
            orderMapper.markRestored(orderId);
            return true;
        });
        if (!Boolean.TRUE.equals(closed)) {
            return false;
        }

        // Redis 回补放在事务外：它不是事务资源，混进去只会让事务时长变长。
        // 活动已结束清理过键时 restore 内部会跳过，不会造出脏键
        stockRedisService.restore(order.getActivityId(), order.getGoodsId(),
                order.getUserId(), order.getQuantity());
        log.info("超时关单完成 orderId={} orderNo={} 回补库存={}",
                orderId, order.getOrderNo(), order.getQuantity());
        return true;
    }

    private String generateOrderNo(Long requestId) {
        String date = DateTimeFormatter.ofPattern("yyyyMMdd").format(LocalDate.now());
        String tail = String.valueOf(requestId);
        return "SK" + date + tail.substring(Math.max(0, tail.length() - 10));
    }

    // ------------------------------------------------------------
    // 内部异常：只用于在事务内传递「该走哪条补偿路径」
    // ------------------------------------------------------------

    static class InsufficientStockException extends RuntimeException {
        InsufficientStockException(String message) {
            super(message);
        }
    }

    static class DuplicateBuyException extends RuntimeException {
        DuplicateBuyException() {
            super("重复购买");
        }
    }
}
