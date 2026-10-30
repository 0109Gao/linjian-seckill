package com.linjian.seckill.domain.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户表。C 端买家与后台管理员共用一张表，通过 role 区分。
 * <p>
 * 取舍说明：单表 + role 字段在规模较小时优于拆成 buyer / admin 两张表，
 * 登录、鉴权、审计逻辑只需要维护一套；若将来需要完全隔离权限模型再拆分。
 */
@Data
@TableName("sk_user")
public class SkUser {

    /** 雪花 ID，避免自增 ID 暴露注册量 */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String username;

    /** BCrypt 加密后的密码，任何接口都不得返回该字段 */
    private String password;

    private String nickname;

    private String phone;

    /** USER：买家；ADMIN：管理员 */
    private String role;

    /** 1 正常，0 禁用 */
    private Integer status;

    @TableLogic
    @TableField(select = false)
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
