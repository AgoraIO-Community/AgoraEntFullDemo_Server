package com.md.service.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 用户黑名单
 * </p>
 */
@Getter
@Setter
@TableName("user_blacklist")
@ApiModel(value = "UserBlacklist对象", description = "用户黑名单")
public class UserBlacklist implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty("id")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @ApiModelProperty("用户手机号")
    private String mobile;

    @ApiModelProperty("加入黑名单原因")
    private String reason;

    @ApiModelProperty("操作人")
    private String operator;

    @ApiModelProperty("创建时间")
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @ApiModelProperty("修改时间")
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}