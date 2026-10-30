package com.linjian.seckill.web.controller;

import com.linjian.seckill.common.PageResult;
import com.linjian.seckill.common.Result;
import com.linjian.seckill.domain.dto.GoodsSaveRequest;
import com.linjian.seckill.domain.dto.GoodsVO;
import com.linjian.seckill.domain.service.GoodsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "后台-商品管理")
@RestController
@RequestMapping("/api/admin/goods")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminGoodsController {

    private final GoodsService goodsService;

    @Operation(summary = "新增商品")
    @PostMapping
    public Result<Long> create(@Valid @RequestBody GoodsSaveRequest request) {
        return Result.success(goodsService.create(request));
    }

    @Operation(summary = "修改商品")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody GoodsSaveRequest request) {
        goodsService.update(id, request);
        return Result.success();
    }

    @Operation(summary = "删除商品", description = "已被活动引用的商品不允许删除")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        goodsService.delete(id);
        return Result.success();
    }

    @Operation(summary = "商品详情")
    @GetMapping("/{id}")
    public Result<GoodsVO> get(@PathVariable Long id) {
        return Result.success(goodsService.get(id));
    }

    @Operation(summary = "商品分页")
    @GetMapping
    public Result<PageResult<GoodsVO>> page(
            @RequestParam(defaultValue = "1") long pageNum,
            @RequestParam(defaultValue = "10") long pageSize,
            @RequestParam(required = false) String keyword) {
        return Result.success(goodsService.page(pageNum, pageSize, keyword));
    }
}
