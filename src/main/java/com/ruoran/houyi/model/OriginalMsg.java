package com.ruoran.houyi.model;

import lombok.Data;

import jakarta.persistence.*;

/**
 * @author renlu
 * created by renlu at 2021/7/14 5:43 下午
 */
@Data
@Entity
@Table(name="original_msg",indexes = {
})
public class OriginalMsg {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    long id;

    @Column(name="corp_id",columnDefinition = "varchar(32)")
    String corpId;
    @Column(name="msg_id",columnDefinition = "varchar(128)")
    String msgId;
    Long seq;
    String content;
    String sdkfileid;
    long dateNo;
    String msgType;
    String filePath;

    long downFinishAt;
    long downFailAt;

    String md5Sum;

    int timeout=0;

    String ossPath;

    long createAt;

    Long pushAt;

}
