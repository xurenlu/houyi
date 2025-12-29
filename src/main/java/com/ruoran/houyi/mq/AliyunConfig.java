package com.ruoran.houyi.mq;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * @author xurenlu
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "aliyun")
public class AliyunConfig {
    String key;
    String secret;
    String address = "http://31732258.mns.cn-shanghai.aliyuncs.com/";

}
