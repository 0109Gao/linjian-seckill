package com.linjian.seckill.domain.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.linjian.seckill.common.BizException;
import com.linjian.seckill.common.ErrorCode;
import com.linjian.seckill.common.PageResult;
import com.linjian.seckill.domain.dto.GoodsSaveRequest;
import com.linjian.seckill.domain.dto.GoodsVO;
import com.linjian.seckill.domain.entity.SkActivityGoods;
import com.linjian.seckill.domain.entity.SkGoods;
import com.linjian.seckill.domain.mapper.SkActivityGoodsMapper;
import com.linjian.seckill.domain.mapper.SkGoodsMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 商品服务。只有后台管理端会调用写操作，C 端只读。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GoodsService {

    private final SkGoodsMapper goodsMapper;
    private final SkActivityGoodsMapper activityGoodsMapper;

    @Transactional(rollbackFor = Exception.class)
    public Long create(GoodsSaveRequest request) {
        SkGoods goods = new SkGoods();
        goods.setName(request.getName());
        goods.setSubtitle(request.getSubtitle());
        goods.setImageUrl(request.getImageUrl());
        goods.setOriginPrice(request.getOriginPrice());
        goods.setStatus(request.getStatus() == null ? 1 : request.getStatus());
        goodsMapper.insert(goods);
        log.info("商品创建成功 goodsId={} name={}", goods.getId(), goods.getName());
        return goods.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, GoodsSaveRequest request) {
        SkGoods exists = goodsMapper.selectById(id);
        if (exists == null) {
            throw new BizException(ErrorCode.GOODS_NOT_FOUND);
        }
        SkGoods goods = new SkGoods();
        goods.setId(id);
        goods.setName(request.getName());
        goods.setSubtitle(request.getSubtitle());
        goods.setImageUrl(request.getImageUrl());
        goods.setOriginPrice(request.getOriginPrice());
        goods.setStatus(request.getStatus());
        // 按需更新：request 里为 null 的字段不覆盖已有值
        goodsMapper.updateById(goods);
    }

    /**
     * 删除商品。被活动引用的商品不允许删除——直接删会让活动详情页出现
     * 「商品不存在」的空洞，而且历史订单的关联也会断掉。
     */
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        Long referenced = activityGoodsMapper.selectCount(
                Wrappers.<SkActivityGoods>lambdaQuery().eq(SkActivityGoods::getGoodsId, id));
        if (referenced != null && referenced > 0) {
            throw new BizException(ErrorCode.GOODS_IN_USE);
        }
        if (goodsMapper.deleteById(id) == 0) {
            throw new BizException(ErrorCode.GOODS_NOT_FOUND);
        }
    }

    public GoodsVO get(Long id) {
        SkGoods goods = goodsMapper.selectById(id);
        if (goods == null) {
            throw new BizException(ErrorCode.GOODS_NOT_FOUND);
        }
        return toVO(goods);
    }

    public PageResult<GoodsVO> page(long pageNum, long pageSize, String keyword) {
        Page<SkGoods> page = new Page<>(pageNum, pageSize);
        IPage<SkGoods> result = goodsMapper.selectPage(page,
                Wrappers.<SkGoods>lambdaQuery()
                        .like(keyword != null && !keyword.isBlank(), SkGoods::getName, keyword)
                        .orderByDesc(SkGoods::getCreateTime));

        return PageResult.of(
                result.getRecords().stream().map(this::toVO).toList(),
                result.getTotal(), result.getCurrent(), result.getSize());
    }

    private GoodsVO toVO(SkGoods goods) {
        GoodsVO vo = new GoodsVO();
        vo.setId(goods.getId());
        vo.setName(goods.getName());
        vo.setSubtitle(goods.getSubtitle());
        vo.setImageUrl(goods.getImageUrl());
        vo.setOriginPrice(goods.getOriginPrice());
        vo.setStatus(goods.getStatus());
        vo.setCreateTime(goods.getCreateTime());
        return vo;
    }
}
