package com.ruoran.houyi.model;

import lombok.Data;
import org.springframework.web.bind.annotation.GetMapping;

import jakarta.persistence.*;

/**
 * @author renlu
 * created by renlu at 2021/7/14 5:26 下午
 */
@Data
@Entity
@Table(name="corplist")
public class CorpInfo {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    long id;
    String corpid;
    String secret;
    String corpname;
    String prikey;
    long limits;
    long timeout;
    long status;
    @Column(name = "`update`")
    long update;
    long lastSeq;
}
