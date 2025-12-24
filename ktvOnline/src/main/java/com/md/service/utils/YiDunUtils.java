package com.md.service.utils;

import com.md.service.common.ErrorCodeEnum;
import com.md.service.exception.BaseException;
import com.netease.yidun.sdk.antispam.image.v5.ImageClient;
import com.netease.yidun.sdk.antispam.image.v5.check.ImageV5CheckRequest;
import com.netease.yidun.sdk.antispam.image.v5.check.sync.request.ImageV5SyncCheckRequest;
import com.netease.yidun.sdk.antispam.image.v5.check.sync.response.ImageV5CheckResponse;
import com.netease.yidun.sdk.antispam.image.v5.check.sync.response.ImageV5Result;
import com.netease.yidun.sdk.antispam.image.v5.check.sync.response.ImageV5AntispamResp;
import com.netease.yidun.sdk.antispam.text.TextClient;
import com.netease.yidun.sdk.antispam.text.v5.check.sync.single.TextCheckRequest;
import com.netease.yidun.sdk.antispam.text.v5.check.sync.single.TextCheckResponse;
import com.netease.yidun.sdk.antispam.text.v5.check.sync.single.TextCheckResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

@Component
@Slf4j
public class YiDunUtils {

    @Resource
    private TextClient textClient;

    @Resource
    private ImageClient imageClient;

    @Value("${yidun.text.businessId:}")
    private String textBusinessId;

    @Value("${yidun.image.businessId:}")
    private String imageBusinessId;

    public void checkYidunText(String msg) {
        if (msg == null || msg.trim().isEmpty()) {
            log.warn("Text to be moderated is empty, skip moderation");
            return;
        }

        try {
            TextCheckRequest request = new TextCheckRequest();
            request.setBusinessId(textBusinessId);
            request.setRegionCode("cn-hangzhou");
            request.setDataId(String.valueOf(System.currentTimeMillis()));
            request.setContent(msg);

            log.info("Yidun text moderation request - businessId: {}, content: {}", textBusinessId, msg);

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

    public void checkImage(String imageUrl) {
        if (imageUrl == null || imageUrl.trim().isEmpty()) {
            log.warn("Image URL to be moderated is empty, skip moderation");
            return;
        }

        try {
            ImageV5SyncCheckRequest request = new ImageV5SyncCheckRequest();
            request.setBusinessId(imageBusinessId);
            request.setRegionCode("cn-hangzhou");

            ImageV5CheckRequest.ImageBeanRequest image = new ImageV5CheckRequest.ImageBeanRequest();
            image.setData(imageUrl);
            image.setName("image_" + System.currentTimeMillis());
            image.setType(1);

            List<ImageV5CheckRequest.ImageBeanRequest> images = new ArrayList<>();
            images.add(image);
            request.setImages(images);

            log.info("Yidun image moderation request - businessId: {}, imageUrl: {}", imageBusinessId, imageUrl);

            ImageV5CheckResponse response = imageClient.syncCheckImage(request);

            if (response == null) {
                log.error("Yidun image moderation API call failed - response is null");
                return;
            }

            log.info("Yidun image moderation response - code: {}", response.getCode());

            if (response.getCode() != 200) {
                log.error("Yidun image moderation API call failed - code: {}, msg: {}", response.getCode(), response.getMsg());
                return;
            }

            if (response.getResult() != null && !response.getResult().isEmpty()) {
                ImageV5Result result = response.getResult().get(0);
                if (result.getAntispam() != null) {
                    ImageV5AntispamResp antispam = result.getAntispam();

                    // suggestion: 0-pass, 1-suspect, 2-reject
                    Integer suggestion = antispam.getSuggestion();
                    if (suggestion != null && suggestion == 2) {
                        log.warn("Yidun detected illegal content - imageUrl: {}", imageUrl);
                        throw new BaseException(ErrorCodeEnum.please_dont_upload_illegal_content);
                    } else if (suggestion != null && suggestion == 1) {
                        log.warn("Yidun detected suspicious image - suggestion: {}, imageUrl: {}", suggestion, imageUrl);
                    }
                }
            }

        } catch (BaseException e) {
            throw e;
        } catch (Exception e) {
            log.error("Yidun image moderation check failed: {}", e.getMessage(), e);
        }
    }

}
