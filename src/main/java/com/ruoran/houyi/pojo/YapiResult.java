package com.ruoran.houyi.pojo;

import lombok.Data;

/**
 * @author renlu
 * created by renlu at 2021/6/29 7:22 下午
 */
@Data
public class YapiResult {
    String msg;
    CorpInfoData data;
    Integer code;
}
