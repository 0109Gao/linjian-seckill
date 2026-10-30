package com.linjian.seckill.web.controller;

import com.linjian.seckill.common.PageResult;
import com.linjian.seckill.common.Result;
import com.linjian.seckill.domain.dto.OrderVO;
import com.linjian.seckill.domain.dto.SeckillResult;
import com.linjian.seckill.domain.service.OrderService;
import com.linjian.seckill.domain.service.SeckillService;
import com.linjian.seckill.security.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * C 端秒杀接口，必须登录。
 */
@Tag(name = "C 端-秒杀")
@RestController
@RequestMapping("/api/seckill")
@RequiredArgsConstructor
public class SeckillController {

    private final SeckillService seckillService;
    private final OrderService orderService;

    @Operation(summary = "发起秒杀",
            description = "返回 PENDING + requestId，前端用 requestId 轮询 /result/{requestId} 拿最终结果")
    @PostMapping("/{activityId}/{goodsId}")
    public Result<SeckillResult> seckill(@PathVariable Long activityId, @PathVariable Long goodsId) {
        return Result.success(seckillService.seckill(activityId, goodsId, SecurityUtils.getCurrentUserId()));
    }

    @Operation(summary = "轮询秒杀结果",
            description = "SUCCESS 时返回 orderId 与 orderNo；FAILED 时返回 code 与 message")
    @GetMapping("/result/{requestId}")
    public Result<SeckillResult> result(@PathVariable Long requestId) {
        return Result.success(seckillService.queryResult(requestId, SecurityUtils.getCurrentUserId()));
    }

    @Operation(summary = "我的订单")
    @GetMapping("/orders")
    public Result<PageResult<OrderVO>> orders(
            @RequestParam(defaultValue = "1") long pageNum,
            @RequestParam(defaultValue = "10") long pageSize) {
        return Result.success(orderService.pageByUser(SecurityUtils.getCurrentUserId(), pageNum, pageSize));
    }

    @Operation(summary = "模拟支付", description = "仅用于验证订单状态机与超时关单的竞争关系")
    @PostMapping("/orders/{orderNo}/pay")
    public Result<OrderVO> pay(@PathVariable String orderNo) {
        return Result.success(orderService.pay(orderNo, SecurityUtils.getCurrentUserId()));
    }
}
