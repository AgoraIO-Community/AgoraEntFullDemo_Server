package com.md.service.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.json.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.md.service.common.*;
import com.md.service.exception.BaseException;
import com.md.service.model.BaseResult;
import com.md.service.model.dto.RealNameAuthDTO;
import com.md.service.model.dto.UserInfo;
import com.md.service.model.entity.RoomInfo;
import com.md.service.model.entity.RoomUsers;
import com.md.service.model.entity.Users;
import com.md.service.model.form.CreateUserVerificationForm;
import com.md.service.model.form.CreateUserVerificationLogForm;
import com.md.service.model.form.UpdateUserInfoForm;
import com.md.service.repository.UsersMapper;
import com.md.service.service.*;
import com.md.service.utils.*;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jetty.util.security.Credential;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/**
 * <p>
 * 用户信息 服务实现类
 * </p>
 */
@Service
public class UsersServiceImpl extends ServiceImpl<UsersMapper, Users> implements UsersService {

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    @Resource
    private AliSmsSingleSender aliSmsSingleSender;

    @Value("${verification.code.time}")
    private Integer codeTime;

    @Value("${verification.send.code.times}")
    private Integer sendMessageTimes;

    @Value("${verification.code.error.lock}")
    private Integer errorLock;

    @Value("${verification.code.error.time}")
    private Integer errorTime;

    @Value("${verification.code.superCode}")
    private String superCodeRegexPattern;

    @Value("${verification.code.superCodeValidation}")
    private boolean superCodeValidation;

    @Value("${realNameAuth.encryptionKey}")
    private String encryptionKey;

    @Value("${realNameAuth.superRealName}")
    private String superRealName;

    @Value("${realNameAuth.superIdCard}")
    private String superIdCard;

    @Resource
    private JwtUtil jwtUtil;

    @Resource
    private YiDunUtils yidunUtils;

    @Resource
    private RoomUsersService roomUsersService;

    @Resource
    private RoomInfoService roomInfoService;

    @Resource
    private UserActionInfoService userActionInfoService;

    @Resource
    private UserConfigService userConfigService;

    @Resource
    private UserDeviceInfoService userDeviceInfoService;

    @Resource
    private RealNameAuthService realNameAuthService;

    @Resource
    private UserVerificationService userVerificationService;

    @Resource
    private UserVerificationLogService userVerificationLogService;

    @Resource
    private RedissonClient redissonClient;

    @Value("${realNameAuth.errTimesThreshold}")
    private Integer realNameAuthErrTimesThreshold;

    @Resource
    private UserBlacklistService userBlacklistService;

    @Override
    public BaseResult<String> verificationCode(String phone) {
        if (userBlacklistService.isMobileBlacklisted(phone)) {
            throw new BaseException(ErrorCodeEnum.user_blacklisted, ErrorCodeEnum.user_blacklisted.getMessage());
        }
        if (redisTemplate.hasKey(phone)) {
            throw new BaseException(ErrorCodeEnum.code_repeat, ErrorCodeEnum.code_repeat.getMessage());
        }
        if (!ValidateUtils.isMobile(phone)) {
            throw new BaseException(ErrorCodeEnum.phone_error, ErrorCodeEnum.phone_error.getMessage());
        }
        if (redisTemplate.hasKey(CommonKey.verificationSendCodeTimes + phone)) {
            String times = String.valueOf(redisTemplate.opsForValue().get(CommonKey.verificationSendCodeTimes + phone));
            if (Integer.parseInt(times) >= sendMessageTimes) {
                throw new BaseException(ErrorCodeEnum.send_code_max);
            }
        }
        redisTemplate.opsForValue().increment(CommonKey.verificationSendCodeTimes + phone);
        redisTemplate.expire(CommonKey.verificationSendCodeTimes + phone, MdStringUtils.getRemainSecondsOneDay(),
                TimeUnit.SECONDS);
        // 发送短信
        String code = MdStringUtils.verificationCode();
        JSONObject jsonObject = new JSONObject();
        jsonObject.set("code", code);
        aliSmsSingleSender.sendCode(phone, jsonObject.toString());
        redisTemplate.opsForValue().set(phone, code, codeTime, TimeUnit.SECONDS);
        return BaseResult.success();
    }

    @Override
    public BaseResult<UserInfo> login(String phone, String code) {
        UserInfo userInfo = new UserInfo();
        if (!redisTemplate.hasKey(phone) && superCodeValidation && !Pattern.matches(superCodeRegexPattern, code)) {
            throw new BaseException(ErrorCodeEnum.no_code, ErrorCodeEnum.no_code.getMessage());
        }
        String redisCode = String.valueOf(redisTemplate.opsForValue().get(phone));
        // 认证成功记录用户信息
        if (StringUtils.isNoneEmpty(code) && code.equals(redisCode)
                || superCodeValidation && Pattern.matches(superCodeRegexPattern, code)) {
            Users users = this.baseMapper.selectOne(new LambdaQueryWrapper<Users>().eq(Users::getMobile, phone));
            if (users == null) {
                users = new Users();
                users.setMobile(phone);
                users.setUserNo(MdStringUtils.randomDelete(Credential.MD5.digest(phone).substring(4), 6));
                users.setName(UserRandomInfo.getName());
                users.setHeadUrl(UserRandomInfo.headImage());
                users.setStatus(0);
                users.setRealNameVerifyStatus(0);
                baseMapper.insert(users);
            }

            // 检查用户是否在黑名单中
            if (userBlacklistService.isMobileBlacklisted(phone)) {
                throw new BaseException(ErrorCodeEnum.user_blacklisted, ErrorCodeEnum.user_blacklisted.getMessage());
            }

            BeanUtil.copyProperties(users, userInfo);
        } else {
            Long times = redisTemplate.opsForValue().increment(CommonKey.verificationCheckCodeTimes + phone);
            redisTemplate.expire(CommonKey.verificationCheckCodeTimes + phone, MdStringUtils.getRemainSecondsOneDay(),
                    TimeUnit.SECONDS);
            if (redisTemplate.hasKey(CommonKey.verificationCheckCodeTimes + phone)) {
                if (times.intValue() >= errorLock) {
                    if (redisTemplate.hasKey(CommonKey.verificationCheckCodeTimesLock + phone)) {
                        throw new BaseException(ErrorCodeEnum.code_error_lock);
                    } else {
                        redisTemplate.opsForValue().set(CommonKey.verificationCheckCodeTimesLock + phone, "1",
                                errorTime, TimeUnit.MINUTES);
                    }
                }
            }
            throw new BaseException(ErrorCodeEnum.code_error);
        }
        String token = jwtUtil.createJWT(userInfo.getUserNo());
        userInfo.setToken(token);
        return BaseResult.success(userInfo);
    }

    @Override
    public BaseResult<UserInfo> updateInfo(UpdateUserInfoForm form) {
        Users users = getUserByNo(form.getUserNo());
        if (StringUtils.isNoneBlank(form.getHeadUrl())) {
            users.setHeadUrl(form.getHeadUrl());
        }
        if (StringUtils.isNoneBlank(form.getName())) {
            yidunUtils.checkYidunText(form.getName());
            users.setName(form.getName());
        }
        if (StringUtils.isNoneBlank(form.getSex())) {
            users.setSex(form.getSex());
        }
        baseMapper.updateById(users);
        UserInfo userInfo = new UserInfo();
        BeanUtil.copyProperties(users, userInfo);
        return BaseResult.success(userInfo);
    }

    @Override
    @Transactional
    public BaseResult<?> cancellation(String no) {
        Users users = getUserByNo(no);
        baseMapper.deleteById(users);
        // 注销后 全部下座位
        roomUsersService.update(new LambdaUpdateWrapper<RoomUsers>().eq(RoomUsers::getUserId, users.getId())
                .set(RoomUsers::getOnSeat, 0).set(RoomUsers::getDeletedAt, LocalDateTime.now()));
        // 关闭所有房间
        roomInfoService.update(new LambdaUpdateWrapper<RoomInfo>().eq(RoomInfo::getCreator, users.getId())
                .set(RoomInfo::getStatus, 1));
        userActionInfoService.clearUserActionInfo(users.getMobile());
        userConfigService.clearUserConfig(users.getMobile());
        userDeviceInfoService.clearUserDeviceInfo(users.getMobile());
        return BaseResult.success();
    }

    @Override
    public BaseResult<UserInfo> getUserInfo(String no) {
        UserInfo userInfo = getUser(no);
        return BaseResult.success(userInfo);
    }

    @Override
    public UserInfo getUser(String no) {
        Users users = getUserByNo(no);
        UserInfo userInfo = new UserInfo();
        BeanUtil.copyProperties(users, userInfo);
        return userInfo;
    }

    @Override
    public Users getUserByNo(String no) {
        Users users = this.baseMapper.selectOne(new LambdaQueryWrapper<Users>().eq(Users::getUserNo, no));
        if (users == null) {
            throw new BaseException(ErrorCodeEnum.user_not_exist, ErrorCodeEnum.user_not_exist.getMessage());
        }
        return users;
    }

    @Override
    public String getUserNoById(Integer id) {
        String userNo = baseMapper.getUserNo(id);
        if (userNo == null) {
            throw new BaseException(ErrorCodeEnum.user_not_exist, ErrorCodeEnum.user_not_exist.getMessage());
        }
        return userNo;
    }

    @Override
    public IPage<Users> userList(Page page) {
        return baseMapper.selectPage(page,
                new LambdaQueryWrapper<Users>().eq(Users::getStatus, RoomStatus.OPEN).orderByDesc(Users::getCreatedAt));
    }

    @Override
    public void realNameAuth(String userNo, String realName, String idCard) throws Exception {
        RLock rlock = redissonClient.getLock(CommonKey.real_name_auth_lock + userNo);
        try {
            if (rlock.tryLock()) {
                realNameAuthWithNoLock(userNo, realName, idCard);
            } else {
                throw new BaseException(ErrorCodeEnum.system_error);
            }
        } catch (InterruptedException e) {
            throw new BaseException(ErrorCodeEnum.real_name_auth_conflict,
                    ErrorCodeEnum.real_name_auth_conflict.getMessage());
        } finally {
            rlock.unlock();
        }
    }

    public void realNameAuthWithNoLock(String userNo, String realName, String idCard) throws Exception {
        // 检查是否在黑名单中
        Boolean hasBlocked = redisTemplate.hasKey(CommonKey.real_name_auth_blacklist + userNo);
        if (hasBlocked) {
            throw new BaseException(ErrorCodeEnum.real_name_auth_exceed_limit,
                    ErrorCodeEnum.real_name_auth_exceed_limit.getMessage());
        }

        Users user = getUserByNo(userNo);
        // 检查是否已经实名认证
        if (Objects.equals(user.getRealNameVerifyStatus(), UserVerificationStatus.Success.getCode())) {
            throw new BaseException(ErrorCodeEnum.real_name_auth_exist,
                    ErrorCodeEnum.real_name_auth_exist.getMessage());
        }

        String bachId = UUID.randomUUID().toString().toLowerCase().replaceAll("-", "");
        String realNameCipher = Utils.aesEncrypt(realName, encryptionKey);
        String idCardCipher = Utils.aesEncrypt(idCard, encryptionKey);

        // 检查是否使用超级验证信息
        if (StringUtils.isNotEmpty(superRealName) && StringUtils.isNotEmpty(superIdCard)
                && realName.equals(superRealName) && idCard.equals(superIdCard)) {
            // 使用超级验证信息，直接通过验证
            userVerificationLogService.createUserVerificationLog(CreateUserVerificationLogForm.builder()
                    .userNo(userNo)
                    .realNameCipher(realNameCipher)
                    .idCardCipher(idCardCipher)
                    .verifyBatchId(bachId)
                    .verifyResult(VerificationResult.Success.getCode())
                    .verifyRemark("Super verification passed")
                    .build());

            userVerificationService.createUserVerification(CreateUserVerificationForm.builder()
                    .userNo(userNo)
                    .verifyBatchId(bachId)
                    .verifyStatus(UserVerificationStatus.Success.getCode())
                    .realNameCipher(realNameCipher)
                    .idCardCipher(idCardCipher)
                    .verifySuccessAt(LocalDateTime.now())
                    .build());

            user.setRealNameVerifyStatus(UserVerificationStatus.Success.getCode());
            baseMapper.updateById(user);
            return;
        }

        // 原有的实名认证逻辑
        RealNameAuthDTO realNameAuthResult = realNameAuthService.auth(realName, idCard);
        if (realNameAuthResult.getCode().equals("200")) {
            if (realNameAuthResult.getResult().equals("1")) {
                userVerificationLogService.createUserVerificationLog(CreateUserVerificationLogForm.builder()
                        .userNo(userNo)
                        .realNameCipher(realNameCipher)
                        .idCardCipher(idCardCipher)
                        .verifyBatchId(bachId)
                        .verifyResult(VerificationResult.Success.getCode())
                        .verifyRemark(JsonUtil.toJsonString(realNameAuthResult))
                        .build());

                userVerificationService.createUserVerification(CreateUserVerificationForm.builder()
                        .userNo(userNo)
                        .verifyBatchId(bachId)
                        .verifyStatus(UserVerificationStatus.Success.getCode())
                        .realNameCipher(realNameCipher)
                        .idCardCipher(idCardCipher)
                        .verifySuccessAt(LocalDateTime.now())
                        .build());

                user.setRealNameVerifyStatus(UserVerificationStatus.Success.getCode());
                baseMapper.updateById(user);

                return;
            }
            // 认证失败,记录日志
            userVerificationLogService.createUserVerificationLog(CreateUserVerificationLogForm.builder()
                    .userNo(userNo)
                    .realNameCipher(realNameCipher)
                    .idCardCipher(idCardCipher)
                    .verifyBatchId(bachId)
                    .verifyResult(VerificationResult.Failed.getCode())
                    .verifyRemark(JsonUtil.toJsonString(realNameAuthResult))
                    .build());
            checkRealNameAuthLimit(userNo);

            throw new BaseException(ErrorCodeEnum.real_name_auth_inconsistent,
                    ErrorCodeEnum.real_name_auth_inconsistent.getMessage());

        }
        userVerificationLogService.createUserVerificationLog(CreateUserVerificationLogForm.builder()
                .userNo(userNo)
                .realNameCipher(realNameCipher)
                .idCardCipher(idCardCipher)
                .verifyBatchId(bachId)
                .verifyResult(VerificationResult.Failed.getCode())
                .verifyRemark(JsonUtil.toJsonString(realNameAuthResult))
                .build());

        if (realNameAuthResult.getCode().equals("401")) {
            checkRealNameAuthLimit(userNo);

            throw new BaseException(ErrorCodeEnum.real_name_auth_param_illegal,
                    ErrorCodeEnum.real_name_auth_param_illegal.getMessage());
        }

        throw new BaseException(ErrorCodeEnum.real_name_auth_unknown_err,
                ErrorCodeEnum.real_name_auth_unknown_err.getMessage());
    }

    private void checkRealNameAuthLimit(String userNo) {
        Boolean existErrTimes = redisTemplate.hasKey(CommonKey.real_name_auth_err_times + userNo);
        if (!existErrTimes) {
            redisTemplate.opsForValue().set(CommonKey.real_name_auth_err_times + userNo, 1,
                    MdStringUtils.getRemainSecondsOneDay(),
                    TimeUnit.SECONDS);
        } else {
            Long errTimes = redisTemplate.opsForValue().increment(CommonKey.real_name_auth_err_times + userNo);
            if (errTimes.intValue() >= realNameAuthErrTimesThreshold) {
                // 超过认证失败次数,加入黑名单，并且设置黑名单时间
                redisTemplate.opsForValue().setIfAbsent(CommonKey.real_name_auth_blacklist + userNo, 1,
                        MdStringUtils.getRemainSecondsOneDay(),
                        TimeUnit.SECONDS);
                throw new BaseException(ErrorCodeEnum.real_name_auth_exceed_limit,
                        ErrorCodeEnum.real_name_auth_exceed_limit.getMessage());
            }
        }
    }

}
