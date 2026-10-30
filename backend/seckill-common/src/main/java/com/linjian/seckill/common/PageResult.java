package com.linjian.seckill.common;

import lombok.Data;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

/**
 * 统一分页结果。
 * <p>
 * 不直接返回 MyBatis-Plus 的 IPage：它的字段名（records / current / size）
 * 与前端约定不一致，且把 ORM 的返回类型暴露到接口上，换 ORM 就要改前端。
 */
@Data
public class PageResult<T> implements Serializable {

    private List<T> list;

    private long total;

    private long pageNum;

    private long pageSize;

    public static <T> PageResult<T> of(List<T> list, long total, long pageNum, long pageSize) {
        PageResult<T> result = new PageResult<>();
        result.setList(list == null ? Collections.emptyList() : list);
        result.setTotal(total);
        result.setPageNum(pageNum);
        result.setPageSize(pageSize);
        return result;
    }
}
