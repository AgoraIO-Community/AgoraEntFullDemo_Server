package com.md.service.config;

import com.netease.yidun.sdk.antispam.AntispamRequester;
import com.netease.yidun.sdk.antispam.image.v5.ImageClient;
import com.netease.yidun.sdk.antispam.text.TextClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
public class YidunConfig {

    @Value("${yidun.text.secretId:}")
    private String textSecretId;

    @Value("${yidun.text.secretKey:}")
    private String textSecretKey;

    @Value("${yidun.text.businessId:}")
    private String textBusinessId;

    @Value("${yidun.image.secretId:}")
    private String imageSecretId;

    @Value("${yidun.image.secretKey:}")
    private String imageSecretKey;

    @Value("${yidun.image.businessId:}")
    private String imageBusinessId;

    @Bean
    public TextClient textClient() {
        if (textSecretId == null || textSecretId.isEmpty() ||
                textSecretKey == null || textSecretKey.isEmpty() ||
                textBusinessId == null || textBusinessId.isEmpty()) {
            throw new IllegalStateException("Yidun text configuration incomplete. Please configure yidun.text.secretId, yidun.text.secretKey, yidun.text.businessId");
        }

        AntispamRequester antispamRequester = AntispamRequester.getInstance(textSecretId, textSecretKey);
        TextClient textClient = TextClient.getInstance(antispamRequester);
        log.info("Yidun text moderation client initialized successfully - businessId: {}", textBusinessId);
        return textClient;
    }

    @Bean
    public ImageClient imageClient() {
        if (imageSecretId == null || imageSecretId.isEmpty() ||
                imageSecretKey == null || imageSecretKey.isEmpty() ||
                imageBusinessId == null || imageBusinessId.isEmpty()) {
            throw new IllegalStateException("Yidun image configuration incomplete. Please configure yidun.image.secretId, yidun.image.secretKey, yidun.image.businessId");
        }

        AntispamRequester antispamRequester = AntispamRequester.getInstance(imageSecretId, imageSecretKey);
        ImageClient imageClient = ImageClient.getInstance(antispamRequester);
        log.info("Yidun image moderation client initialized successfully - businessId: {}", imageBusinessId);
        return imageClient;
    }

}
