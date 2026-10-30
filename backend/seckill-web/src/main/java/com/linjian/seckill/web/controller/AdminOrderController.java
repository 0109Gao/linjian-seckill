package com.linjian.seckill.web.controller;

import com.linjian.seckill.common.PageResult;
import com.linjian.seckill.common.Result;
import com.linjian.seckill.domain.dto.AdminOrderVO;
import com.linjian.seckill.domain.service.AdminOrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 后台订单查询，只读。
 */
@Tag(name = "后台-订单查询")
@RestController
@RequestMapping("/api/admin/orders")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminOrderController {

    private final AdminOrderService adminOrderService;

    @Operation(summary = "订单分页", description = "支持按活动、状态、订单号、用户筛选；status：0 待支付 1 已支付 2 已取消 3 已关闭")
    @GetMapping
    public Result<PageResult<AdminOrderVO>> page(
            @RequestParam(defaultValue = "1") long pageNum,
            @RequestParam(defaultValue = "10") long pageSize,
            @RequestParam(required = false) Long activityId,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String orderNo,
            @RequestParam(required = false) Long userId) {
        return Result.success(adminOrderService.page(pageNum, pageSize, activityId, status, orderNo, userId));
    }
}
