package com.linjian.seckill.domain.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.linjian.seckill.common.PageResult;
import com.linjian.seckill.domain.dto.AdminOrderVO;
import com.linjian.seckill.domain.entity.SkActivity;
import com.linjian.seckill.domain.entity.SkSeckillOrder;
import com.linjian.seckill.domain.entity.SkUser;
import com.linjian.seckill.domain.enums.OrderStatus;
import com.linjian.seckill.domain.mapper.SkActivityMapper;
import com.linjian.seckill.domain.mapper.SkSeckillOrderMapper;
import com.linjian.seckill.domain.mapper.SkUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 后台订单查询。
 * <p>
 * 只读，不涉及状态机。订单的支付与关单走 {@link OrderService} 与定时任务，
 * 后台不给「手工改订单状态」的入口——真要有这需求也该走退款流程，
 * 而不是让运营把一条已支付订单点成已关闭。
 */
@Service
@RequiredArgsConstructor
public class AdminOrderService {

    private final SkSeckillOrderMapper orderMapper;
    private final SkUserMapper userMapper;
    private final SkActivityMapper activityMapper;

    /**
     * 订单分页。
     * <p>
     * username 与 activityTitle 用两次批量查询补全，而不是写 JOIN：MyBatis-Plus 的
     * 条件构造器只对主表生效，手写 JOIN 就得放弃它，转而维护一坨 XML。
     * 一页 10~20 条，两次 IN 查询的代价远小于维护成本。
     */
    public PageResult<AdminOrderVO> page(long pageNum, long pageSize, Long activityId,
                                         Integer status, String orderNo, Long userId) {
        Page<SkSeckillOrder> page = new Page<>(pageNum, pageSize);
        IPage<SkSeckillOrder> result = orderMapper.selectPage(page,
                Wrappers.<SkSeckillOrder>lambdaQuery()
                        .eq(activityId != null, SkSeckillOrder::getActivityId, activityId)
                        .eq(status != null, SkSeckillOrder::getStatus, status)
                        .eq(userId != null, SkSeckillOrder::getUserId, userId)
                        .like(StringUtils.hasText(orderNo), SkSeckillOrder::getOrderNo, orderNo)
                        .orderByDesc(SkSeckillOrder::getCreateTime));

        List<SkSeckillOrder> records = result.getRecords();
        if (records.isEmpty()) {
            return PageResult.of(Collections.emptyList(), result.getTotal(), result.getCurrent(), result.getSize());
        }

        Map<Long, String> userNames = loadUserNames(records);
        Map<Long, String> activityTitles = loadActivityTitles(records);

        List<AdminOrderVO> list = records.stream()
                .map(order -> toVO(order, userNames, activityTitles))
                .toList();
        return PageResult.of(list, result.getTotal(), result.getCurrent(), result.getSize());
    }

    private Map<Long, String> loadUserNames(List<SkSeckillOrder> records) {
        Set<Long> ids = records.stream().map(SkSeckillOrder::getUserId).collect(Collectors.toSet());
        if (ids.isEmpty()) {
            return Collections.emptyMap();
        }
        return userMapper.selectBatchIds(ids).stream()
                .collect(Collectors.toMap(SkUser::getId,
                        user -> StringUtils.hasText(user.getNickname()) ? user.getNickname() : user.getUsername(),
                        (a, b) -> a));
    }

    private Map<Long, String> loadActivityTitles(List<SkSeckillOrder> records) {
        Set<Long> ids = records.stream().map(SkSeckillOrder::getActivityId).collect(Collectors.toSet());
        if (ids.isEmpty()) {
            return Collections.emptyMap();
        }
        return activityMapper.selectBatchIds(ids).stream()
                .collect(Collectors.toMap(SkActivity::getId, SkActivity::getTitle, (a, b) -> a));
    }

    private AdminOrderVO toVO(SkSeckillOrder order, Map<Long, String> userNames, Map<Long, String> activityTitles) {
        AdminOrderVO vo = new AdminOrderVO();
        vo.setId(order.getId());
        vo.setOrderNo(order.getOrderNo());
        vo.setRequestId(order.getRequestId());
        vo.setUserId(order.getUserId());
        vo.setUsername(userNames.getOrDefault(order.getUserId(), "-"));
        vo.setActivityId(order.getActivityId());
        vo.setActivityTitle(activityTitles.getOrDefault(order.getActivityId(), "-"));
        vo.setGoodsId(order.getGoodsId());
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
}
