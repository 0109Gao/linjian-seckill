package com.linjian.seckill.web.controller;

import com.linjian.seckill.common.Result;
import com.linjian.seckill.domain.dto.DashboardOverviewVO;
import com.linjian.seckill.domain.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "后台-监控看板")
@RestController
@RequestMapping("/api/admin/dashboard")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminDashboardController {

    private final DashboardService dashboardService;

    @Operation(summary = "看板总览", description = "活动/商品/订单计数、成交额，以及活动维度明细")
    @GetMapping("/overview")
    public Result<DashboardOverviewVO> overview(@RequestParam(defaultValue = "10") int activityLimit) {
        return Result.success(dashboardService.overview(activityLimit));
    }
}
