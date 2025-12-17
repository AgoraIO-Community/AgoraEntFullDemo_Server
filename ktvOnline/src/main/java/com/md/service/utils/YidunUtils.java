package com.md.service.utils;

import com.md.service.common.ErrorCodeEnum;
import com.md.service.exception.BaseException;
import com.netease.yidun.sdk.antispam.AntispamRequester;
import com.netease.yidun.sdk.antispam.text.TextCheckClient;
import com.netease.yidun.sdk.antispam.text.v5.check.sync.single.TextCheckRequest;
import com.netease.yidun.sdk.antispam.text.v5.check.sync.single.TextCheckResponse;
import com.netease.yidun.sdk.antispam.text.v5.check.sync.single.TextCheckResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

@Component
@Slf4j
public class YidunUtils {

    @Value("${yidun.secretId:}")
    private String secretId;

    @Value("${yidun.secretKey:}")
    private String secretKey;

    @Value("${yidun.businessId:}")
    private String businessId;

    private TextCheckClient textClient;
    private AntispamRequester antispamRequester;

    @PostConstruct
    public void init() {
        if (secretId == null || secretId.isEmpty() ||
                secretKey == null || secretKey.isEmpty() ||
                businessId == null || businessId.isEmpty()) {
            log.warn("Yidun configuration incomplete, YidunUtils will not work. Please configure yidun.secretId, yidun.secretKey, yidun.businessId");
            return;
        }

        try {
            antispamRequester = AntispamRequester.getInstance(secretId, secretKey);
            textClient = antispamRequester.getTextCheckClient();
            log.info("Yidun text moderation client initialized successfully - businessId: {}", businessId);
        } catch (Exception e) {
            log.error("Yidun text moderation client initialization failed: {}", e.getMessage(), e);
        }
    }

    @PreDestroy
    public void destroy() {
        if (antispamRequester != null) {
            try {
                textClient = null;
                antispamRequester = null;
                log.info("Yidun text moderation client closed");
            } catch (Exception e) {
                log.error("Failed to close Yidun text moderation client: {}", e.getMessage());
            }
        }
    }

    public void checkYidunText(String msg) {
        if (textClient == null) {
            log.warn("Yidun client not initialized, skip text moderation");
            return;
        }

        if (msg == null || msg.trim().isEmpty()) {
            log.warn("Text to be moderated is empty, skip moderation");
            return;
        }

        try {
            TextCheckRequest request = new TextCheckRequest();
            request.setDataId(String.valueOf(System.currentTimeMillis()));
            request.setContent(msg);
            request.setBusinessId(businessId);

            log.info("Yidun text moderation request - businessId: {}, content: {}", businessId, msg);

            TextCheckResponse response = textClient.syncCheckText(request);

            if (response == null) {
                log.error("Yidun text moderation API call failed - response is null");
                return;
            }

            log.info("Yidun text moderation response - code: {}", response.getCode());

            if (response.getCode() != 200) {
                log.error("Yidun text moderation API call failed - code: {}, msg: {}", response.getCode(), response.getMsg());
                return;
            }

            TextCheckResult result = response.getResult();
            if (result != null && result.getAntispam() != null) {
                TextCheckResult.Antispam antispam = result.getAntispam();

                // suggestion: 0-pass, 1-suspect, 2-reject
                Integer suggestion = antispam.getSuggestion();
                if (suggestion != null && suggestion == 2) {
                    log.warn("Yidun detected illegal content - suggestion: {}, content: {}", suggestion, msg);
                    throw new BaseException(ErrorCodeEnum.please_dont_upload_illegal_content);
                } else if (suggestion != null && suggestion == 1) {
                    log.warn("Yidun detected suspicious content - suggestion: {}, content: {}", suggestion, msg);
                }

                if (antispam.getLabels() != null && !antispam.getLabels().isEmpty()) {
                    log.debug("Yidun detected labels: {}", antispam.getLabels());
                }
            }

        } catch (BaseException e) {
            throw e;
        } catch (Exception e) {
            log.error("Yidun text moderation check failed: {}", e.getMessage(), e);
        }
    }
}
