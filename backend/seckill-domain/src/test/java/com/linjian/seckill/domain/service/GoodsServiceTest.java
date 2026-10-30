package com.linjian.seckill.domain.service;

import com.linjian.seckill.common.BizException;
import com.linjian.seckill.common.ErrorCode;
import com.linjian.seckill.domain.entity.SkActivityGoods;
import com.linjian.seckill.domain.mapper.SkActivityGoodsMapper;
import com.linjian.seckill.domain.mapper.SkGoodsMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GoodsServiceTest {

    @Mock
    private SkGoodsMapper goodsMapper;

    @Mock
    private SkActivityGoodsMapper activityGoodsMapper;

    @InjectMocks
    private GoodsService goodsService;

    @Test
    @DisplayName("被活动引用的商品不允许删除")
    void deleteRejectsReferencedGoods() {
        when(activityGoodsMapper.selectCount(any())).thenReturn(2L);

        assertThatThrownBy(() -> goodsService.delete(2001L))
                .isInstanceOf(BizException.class)
                .extracting("code")
                .isEqualTo(ErrorCode.GOODS_IN_USE.getCode());

        verify(goodsMapper, never()).deleteById(any(Long.class));
    }

    @Test
    @DisplayName("未被引用的商品删除时影响行数为 0 应报不存在")
    void deleteRejectsMissingGoods() {
        when(activityGoodsMapper.selectCount(any())).thenReturn(0L);
        when(goodsMapper.deleteById(2001L)).thenReturn(0);

        assertThatThrownBy(() -> goodsService.delete(2001L))
                .isInstanceOf(BizException.class)
                .extracting("code")
                .isEqualTo(ErrorCode.GOODS_NOT_FOUND.getCode());
    }

    @Test
    @DisplayName("未被引用的商品可正常删除")
    void deleteWorksForUnreferencedGoods() {
        when(activityGoodsMapper.selectCount(any())).thenReturn(0L);
        when(goodsMapper.deleteById(2001L)).thenReturn(1);

        goodsService.delete(2001L);

        verify(goodsMapper).deleteById(2001L);
    }
}
