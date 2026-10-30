package com.linjian.seckill.web.controller;

import com.linjian.seckill.common.PageResult;
import com.linjian.seckill.common.Result;
import com.linjian.seckill.domain.dto.ActivityDetailVO;
import com.linjian.seckill.domain.dto.ActivityVO;
import com.linjian.seckill.domain.service.ActivityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * C 端活动接口，无需登录。
 * <p>
 * 只暴露读接口：秒杀下单在 W3 实现，届时会单独开一个需要登录的控制器。
 */
@Tag(name = "C 端-活动")
@RestController
@RequestMapping("/api/activities")
@RequiredArgsConstructor
public class ActivityController {

    private final ActivityService activityService;

    @Operation(summary = "活动列表", description = "status：0 待开始，1 进行中，2 已结束，3 已取消；不传返回全部")
    @GetMapping
    public Result<PageResult<ActivityVO>> page(
            @RequestParam(defaultValue = "1") long pageNum,
            @RequestParam(defaultValue = "10") long pageSize,
            @RequestParam(required = false) Integer status) {
        return Result.success(activityService.page(pageNum, pageSize, status));
    }

    @Operation(summary = "活动详情", description = "items 中的 remaining 为 Redis 剩余库存，null 表示尚未预热")
    @GetMapping("/{id}")
    public Result<ActivityDetailVO> detail(@PathVariable Long id) {
        return Result.success(activityService.detail(id));
    }
}
