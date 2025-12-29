package com.ruoran.houyi.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 企业微信配置
 * 从配置文件读取所有企业的信息，替代外部 API 调用
 *
 * @author refactored
 */
@Data
@Component
@ConfigurationProperties(prefix = "wework")
public class WeWorkCorpProperties {

    /**
     * 企业微信配置列表
     */
    private List<CorpConfig> corps = new ArrayList<>();

    /**
     * 单个企业配置
     */
    @Data
    public static class CorpConfig {
        /**
         * 企业 ID
         */
        private String corpId;

        /**
         * 企业名称
         */
        private String corpName;

        /**
         * 会话存档密钥
         */
        private String secret;

        /**
         * 会话存档私钥
         */
        private String privateKey;

        /**
         * 是否启用 (默认启用)
         */
        private Boolean enabled = true;
    }
}

