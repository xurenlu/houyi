package com.ruoran.houyi.service;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoran.houyi.pojo.YapiResult;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.stereotype.Service;

/**
 * @author renlu
 * created by renlu at 2021/7/15 1:58 下午
 */
@Service
@Slf4j
public class CorpInfoApi {
    ObjectMapper objectMapper;
    public CorpInfoApi() {
        objectMapper = new ObjectMapper();
        objectMapper.disable(DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES);
        objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    }

    /**
     * 获取企业信息（已废弃）
     * 
     * @deprecated 此方法已废弃，企业配置现在从本地配置文件读取
     * @see com.ruoran.houyi.service.CorpConfigService
     */
    @Deprecated
    public YapiResult getCorpInfo(String corpId) throws Exception {
        throw new UnsupportedOperationException(
            "此方法已废弃，企业配置现在从本地配置文件读取，请使用 CorpConfigService"
        );
    }
}
