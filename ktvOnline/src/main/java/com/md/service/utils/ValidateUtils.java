package com.md.service.utils;

import org.apache.commons.lang3.StringUtils;

import java.util.regex.Pattern;

public class ValidateUtils {
    private static final String REGEX_MOBILE = "^((13[0-9])|(14[0-1,4-9])|(15[0-3,5-9])|(16[2,5-7])|(17[0-3,5-8])|(18[0-9])|(19)[0-3,5-9])\\d{8}$";
    /**
     * 判断是否是⼿机号
     * @param tel ⼿机号
     * @return boolean true:是  false:否
     */
    public static boolean isMobile(String tel) {
        if (StringUtils.isEmpty(tel)) {
            return false;
        }
        return Pattern.matches(REGEX_MOBILE, tel);
    }
}