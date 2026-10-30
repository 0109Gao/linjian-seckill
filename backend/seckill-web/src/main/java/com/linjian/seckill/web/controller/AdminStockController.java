package com.linjian.seckill.web.controller;

import com.linjian.seckill.common.Result;
import com.linjian.seckill.domain.dto.ActivityDetailVO;
import com.linjian.seckill.domain.dto.StockAdjustRequest;
import com.linjian.seckill.domain.dto.StockAdjustVO;
import com.linjian.seckill.domain.service.ActivityService;
import com.linjian.seckill.domain.service.AdminStockService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 后台库存管理。
 * <p>
 * 查看复用 C 端的活动详情（它已经带上了 DB 库存与 Redis 余量），
 * 没必要为了「后台视角」再写一个字段完全一样的接口。
 */
@Tag(name = "后台-库存管理")
@RestController
@RequestMapping("/api/admin/stock")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminStockController {

    private final AdminStockService adminStockService;
    private final ActivityService activityService;

    @Operation(summary = "活动库存明细", description = "items 中 stock 为 DB 库存，remaining 为 Redis 可售余量（null 表示未预热）")
    @GetMapping("/activity/{activityId}")
    public Result<ActivityDetailVO> byActivity(@PathVariable Long activityId) {
        return Result.success(activityService.detail(activityId));
    }

    @Operation(summary = "调整库存",
            description = "delta 为增量，正数加、负数减。DB 与 Redis 同时调整，返回 redisSynced 说明同步是否成功")
    @PostMapping("/adjust")
    public Result<StockAdjustVO> adjust(@Valid @RequestBody StockAdjustRequest request) {
        return Result.success(adminStockService.adjust(request));
    }
}
