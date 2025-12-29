package com.ruoran.houyi.pojo;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * @author renlu
 * created by renlu at 2021/6/29 7:20 下午
 */
@Data
public class CorpInfoData {
    long id;
    @JsonProperty("corp_id")
    String corpId;
    @JsonProperty("server_id")
    long serverId;
    @JsonProperty("contact_secret")
    String contactSecret;
    @JsonProperty("customer_secret")
    String customerSecret;

    @JsonProperty("callback_token")
    String callbackToken;
    @JsonProperty("callback_key")
    String callbackAesKey;

    @JsonProperty("message_key")
    String messageKey;

    @JsonProperty("message_secret")
    String messageSecret;

    @JsonProperty("corp_name")
    String corpName;
    int status;
}
