package com.ruoran.houyi.model;

import lombok.Data;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import jakarta.persistence.*;

/**
 * @author renlu
 * created by renlu at 2021/7/15 11:42 上午
 */
@Table(name="md5index",
indexes = {
        @Index(name="idx_md5",columnList = "md5"),
        @Index(name="idx_filepath",columnList = "file_path"),
        @Index(name="idx_oss_at",columnList = "oss_at")
})
@Entity
@Data
@Cacheable
@org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.READ_ONLY)
public class Md5Index {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    long id;
    @Column(name="md5",columnDefinition = "varchar(32)")
    String md5;
    @Column(name="file_path",columnDefinition = "varchar(250)")
    String filePath;
    @Column(name="oss_path",columnDefinition = "varchar(1024)")
    String ossPath;
    @Column(name="oss_at")
    long ossAt=0;

    int times=1;
}
