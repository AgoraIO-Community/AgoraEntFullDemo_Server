package com.md.service.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.md.service.model.entity.UserBlacklist;

/**
 * <p>
 * 用户黑名单 服务类
 * </p>
 */
public interface UserBlacklistService extends IService<UserBlacklist> {

    /**
     * 检查手机号是否在黑名单中
     * 
     * @param mobile 手机号
     * @return true:在黑名单中 false:不在黑名单中
     */
    boolean isMobileBlacklisted(String mobile);
}