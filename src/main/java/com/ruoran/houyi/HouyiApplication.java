package com.ruoran.houyi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * 企业微信会话存档系统主应用
 * 
 * @author xurenlu
 * @author refactored
 */
@SpringBootApplication
@EnableCaching
@EnableScheduling
@EnableTransactionManagement
public class HouyiApplication {

    public static void main(String[] args) {
        SpringApplication.run(HouyiApplication.class, args);
    }

}
