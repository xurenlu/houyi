package com.ruoran.houyi.config;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

/**
 * @author renlu
 * created by renlu at 2021/7/14 5:40 下午
 */
@Configuration
public class JedisConfig {
    @Value("${spring.redis.host}")
    private String host;

    @Value("${spring.redis.port}")
    private Integer port;

    @Value("${spring.redis.password}")
    private String password;

    @Value("${spring.redis.database:0}")
    private Integer database;

    @Bean
    JedisPool jedisPool(){
        GenericObjectPoolConfig<Jedis> poolConfig = new GenericObjectPoolConfig<>();
        poolConfig.setMinIdle(8);
        poolConfig.setMaxTotal(128);
        poolConfig.setMaxIdle(32);
        if(StringUtils.isEmpty(password)){
            return new JedisPool(poolConfig, host, port, database);
        }else {
            return new JedisPool(poolConfig, host, port, database, password);
        }
    }
}
