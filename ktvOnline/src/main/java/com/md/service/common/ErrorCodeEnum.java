package com.md.service.common;

import lombok.Getter;

@Getter
public enum ErrorCodeEnum {

    system_error(10000, "系统异常，请稍后再试"),
    /**
     * 用户
     */
    phone_error(10001, "请输入正确的手机号"),
    code_repeat(10002, "请勿重复发送验证码"),
    no_code(10003, "请重新发送验证"),
    user_not_exist(10004, "用户不存在"),
    verification_code_sent_failure(10005, "验证码发送失败"),
    send_code_max(10006, "验证获取次数已达上限，请明天再试"),
    code_error(10007, "验证码不正确"),
    code_error_lock(10008, "请5分钟后再尝试"),
    real_name_auth_conflict(10009, "用户认证冲突"),
    real_name_auth_exist(10010, "用户已存在实名认证信息"),
    real_name_auth_unknown_err(10011, "用户实名认证失败，未知错误"),
    real_name_auth_inconsistent(10012, "用户实名认证校验不⼀致"),
    real_name_auth_param_illegal(10013, "用户实名参数非法"),
    real_name_auth_exceed_limit(10014, "用户实名认证次数超限"),
    user_blacklisted(10015, "登录异常，有问题请联系 se-product@agora.io"),

    /**
     * 房间
     */
    room_name_is_empty(20001, "房间名称为空"),
    password(20002, "私有房间请输出密码"),
    there_is_no_closed_room(20003, "存在未关闭的房间"),
    cannot_close_room(20004, "无法关闭房间"),
    songs_have_been_switch(20005, "歌曲已经被切了"),
    no_song(20006, "歌曲不存在"),
    no_room(20007, "房间不存在"),
    seat_was(20008, "座位上有人"),
    password_is_not_correct(20009, "密码不正确"),
    please_enter_password(20010, "请输入密码"),

    /**
     * 上传
     */
    upload_failed(90001, "上传图片失败"),

    /**
     * 请不要上传非法内容
     */
    please_dont_upload_illegal_content(90002, "请不要上传非法内容"),

    please_dont_upload_contains_politically_sensitive_content(90003, "请不要上传包含政治敏感内容"),

    invalid_file_format(90004, "无效的文件格式"),

    mic_init_error(50000, "麦位初始化失败"),
    mic_init_already(50001, "麦位已经初始化"),
    add_mic_apply_error(50002, "申请上麦失败"),
    no_mic_apply_record(50003, "没有申请上麦记录"),
    mic_not_init(50004, "麦位未初始化"),
    mic_not_belong_you(50005, "麦位不属于你"),

    mic_is_lock(50006, "麦位已上锁"),
    mic_is_concurrent_operation(50007, "麦位正在被其他人修改"),
    mic_is_cannot_be_modified(50008, "麦位当前状态不允许被修改为目标状态"),
    mic_not_current_user(50009, "麦位不是当前人"),
    mic_index_is_not_null(50010, "麦位索引不允许为空"),
    mic_index_is_not_free(50011, "麦位不是空闲麦位");
    ;

    private final Integer code;

    private final String message;

    ErrorCodeEnum(Integer code, String message) {
        this.code = code;
        this.message = message;
    }
}
