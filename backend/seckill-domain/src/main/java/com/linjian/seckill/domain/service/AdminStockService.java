package com.linjian.seckill.domain.service;

import com.linjian.seckill.common.BizException;
import com.linjian.seckill.common.ErrorCode;
import com.linjian.seckill.domain.dto.StockAdjustRequest;
import com.linjian.seckill.domain.dto.StockAdjustVO;
import com.linjian.seckill.domain.entity.SkActivityGoods;
import com.linjian.seckill.domain.entity.SkGoods;
import com.linjian.seckill.domain.mapper.SkActivityGoodsMapper;
import com.linjian.seckill.domain.mapper.SkGoodsMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 人工库存调整。
 * <p>
 * 这套系统的库存有两份：DB 的 {@code sk_activity_goods.stock} 是最终事实，
 * Redis 的 {@code sk:stock:{activityId}:{goodsId}} 是抢购时真正被扣的那份。
 * 只改一处必然导致「管理员看见补了 100 件，用户抢到 60 件就售罄」，
 * 所以两边都要动，并且把同步失败的情形明确告诉调用方。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminStockService {

    private final SkActivityGoodsMapper activityGoodsMapper;
    private final SkGoodsMapper goodsMapper;
    private final StockRedisService stockRedisService;

    public StockAdjustVO adjust(StockAdjustRequest request) {
        Integer delta = request.getDelta();
        if (delta == null || delta == 0) {
            throw new BizException(ErrorCode.BAD_REQUEST, "调整数量不能为 0");
        }

        SkActivityGoods relation = activityGoodsMapper.selectById(request.getActivityGoodsId());
        if (relation == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "活动商品不存在 id=" + request.getActivityGoodsId());
        }

        int stockBefore = relation.getStock();
        // 负库存的判断交给 SQL 的 stock + delta >= 0，不做「先查再算」：
        // 查与改之间可能有订单落库把库存扣走，算出来的结果立刻就过期了
        if (activityGoodsMapper.adjustStock(relation.getId(), delta) == 0) {
            throw new BizException(ErrorCode.BAD_REQUEST,
                    "调整后库存会变成负数，当前可减 " + stockBefore);
        }

        SkActivityGoods after = activityGoodsMapper.selectById(relation.getId());
        SkGoods goods = goodsMapper.selectById(relation.getGoodsId());

        StockAdjustVO vo = new StockAdjustVO();
        vo.setActivityGoodsId(relation.getId());
        vo.setGoodsId(relation.getGoodsId());
        vo.setGoodsName(goods == null ? "-" : goods.getName());
        vo.setStockBefore(stockBefore);
        vo.setStockAfter(after == null ? null : after.getStock());

        Long remaining = stockRedisService.remaining(relation.getActivityId(), relation.getGoodsId());
        if (remaining == null) {
            vo.setRedisRemaining(null);
            vo.setRedisSynced(false);
            vo.setMessage("该活动尚未预热，只调整了 DB 库存；预热任务会按新库存写入 Redis");
            log.info("库存调整（仅 DB）activityGoodsId={} delta={} {} -> {}",
                    relation.getId(), delta, stockBefore, vo.getStockAfter());
            return vo;
        }

        long target = remaining + delta;
        if (target < 0) {
            // DB 已经挡住负库存，这里还能变负说明 Redis 余量小于 DB 余量
            // （有消息在途未落库）。夹到 0，宁可少卖也不造出负可售量
            target = 0;
        }
        // 用 resetStock 而不是 INCRBY：resetStock 会先校验 TTL 存在，
        // 键已过期时宁可不同步也不造出一个永不过期的库存键
        boolean synced = stockRedisService.resetStock(relation.getActivityId(), relation.getGoodsId(), (int) target);

        vo.setRedisSynced(synced);
        vo.setRedisRemaining(synced ? target : remaining);
        if (!synced) {
            vo.setMessage("DB 已调整，但 Redis 库存键无有效 TTL，未同步；建议对该活动重新预热或等待活动结束清理");
            log.warn("库存调整 Redis 同步失败 activityGoodsId={} delta={}", relation.getId(), delta);
        } else if (target == 0 && remaining + delta < 0) {
            vo.setMessage("DB 已调整；Redis 可售余量已被夹到 0（存在在途未落库的订单）");
        } else {
            vo.setMessage("调整成功，DB 与 Redis 已同步");
            log.info("库存调整 activityGoodsId={} delta={} DB {}->{} Redis {}->{}",
                    relation.getId(), delta, stockBefore, vo.getStockAfter(), remaining, target);
        }
        return vo;
    }
}
