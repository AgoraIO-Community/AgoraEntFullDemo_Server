package com.md.service.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.md.service.model.entity.UserBlacklist;
import com.md.service.repository.UserBlacklistMapper;
import com.md.service.service.UserBlacklistService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Random;
import java.util.concurrent.TimeUnit;

import javax.annotation.Resource;

/**
 * <p>
 * 用户黑名单 服务实现类
 * </p>
 */
@Slf4j
@Service
public class UserBlacklistServiceImpl extends ServiceImpl<UserBlacklistMapper, UserBlacklist>
        implements UserBlacklistService {

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    @Override
    public boolean isMobileBlacklisted(String mobile) {
        if (mobile == null) {
            return false;
        }
        // get from cache
        String redisKey = "blacklist:mobile:" + mobile;
        Object cachedValue = redisTemplate.opsForValue().get(redisKey);
        if (cachedValue != null) {
            return !"EMPTY".equals(cachedValue);
        }

        // get from database
        LambdaQueryWrapper<UserBlacklist> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(UserBlacklist::getMobile, mobile).last("limit 1");
        UserBlacklist userBlacklist = this.getOne(queryWrapper);
        if (userBlacklist != null) {
            // cache a random time within 12 hours to avoid large number of keys expiring at
            // the same time
            int randomTime = 12 + new Random().nextInt(12);
            redisTemplate.opsForValue().set(redisKey, "OK", randomTime,
                    TimeUnit.HOURS);
            log.info("mobile {} is in blacklist,it will be expired in {} hours", mobile, randomTime);
            return true;
        }

        // cache empty value to avoid repeated queries
        int randomTime = 5 + new Random().nextInt(5);
        redisTemplate.opsForValue().set(redisKey, "EMPTY", randomTime, TimeUnit.MINUTES);
        log.info("mobile {} is not in blacklist,empty value will be expired in {} minutes", mobile, randomTime);

        return false;
    }
}