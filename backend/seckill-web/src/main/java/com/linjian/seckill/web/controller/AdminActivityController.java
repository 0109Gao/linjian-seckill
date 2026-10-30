package com.linjian.seckill.web.controller;

import com.linjian.seckill.common.PageResult;
import com.linjian.seckill.common.Result;
import com.linjian.seckill.domain.dto.ActivitySaveRequest;
import com.linjian.seckill.domain.dto.ActivityVO;
import com.linjian.seckill.domain.service.ActivityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "后台-活动管理")
@RestController
@RequestMapping("/api/admin/activities")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminActivityController {

    private final ActivityService activityService;

    @Operation(summary = "创建活动", description = "一活动多商品，items 至少一个")
    @PostMapping
    public Result<Long> create(@Valid @RequestBody ActivitySaveRequest request) {
        return Result.success(activityService.create(request));
    }

    @Operation(summary = "修改活动", description = "仅待开始状态可改")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody ActivitySaveRequest request) {
        activityService.update(id, request);
        return Result.success();
    }

    @Operation(summary = "取消活动", description = "同时清理 Redis 中的库存键")
    @PostMapping("/{id}/cancel")
    public Result<Void> cancel(@PathVariable Long id) {
        activityService.cancel(id);
        return Result.success();
    }

    @Operation(summary = "手动预热库存", description = "定时任务每 30 秒扫描一次，这个接口用于演示与排查；返回本次实际写入的商品数")
    @PostMapping("/{id}/preheat")
    public Result<Integer> preheat(@PathVariable Long id) {
        return Result.success(activityService.preheat(id));
    }

    @Operation(summary = "活动分页（后台）")
    @GetMapping
    public Result<PageResult<ActivityVO>> page(
            @RequestParam(defaultValue = "1") long pageNum,
            @RequestParam(defaultValue = "10") long pageSize,
            @RequestParam(required = false) Integer status) {
        return Result.success(activityService.page(pageNum, pageSize, status));
    }
}
