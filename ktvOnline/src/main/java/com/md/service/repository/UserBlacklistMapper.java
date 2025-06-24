package com.md.service.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.md.service.model.entity.UserBlacklist;
import org.apache.ibatis.annotations.Mapper;

/**
 * <p>
 * 用户黑名单 Mapper 接口
 * </p>
 */
@Mapper
public interface UserBlacklistMapper extends BaseMapper<UserBlacklist> {

}