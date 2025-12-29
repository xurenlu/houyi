package com.ruoran.houyi.utils;

import lombok.extern.slf4j.Slf4j;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.util.Optional;
import java.util.function.Function;

/**
 * Jedis 资源管理工具类
 * 统一处理 Jedis 资源的获取和释放，避免代码重复
 *
 * @author refactored
 */
@Slf4j
public class JedisUtil {

    /**
     * 执行 Jedis 操作，自动管理资源
     *
     * @param jedisPool Jedis 连接池
     * @param operation 要执行的操作
     * @param <T>       返回值类型
     * @return 操作结果
     */
    public static <T> Optional<T> execute(JedisPool jedisPool, Function<Jedis, T> operation) {
        Jedis jedis = null;
        try {
            jedis = jedisPool.getResource();
            return Optional.ofNullable(operation.apply(jedis));
        } catch (Exception e) {
            log.error("Jedis操作失败", e);
            return Optional.empty();
        } finally {
            if (jedis != null) {
                jedisPool.returnResource(jedis);
            }
        }
    }

    /**
     * 执行 Jedis 操作，不返回结果
     *
     * @param jedisPool Jedis 连接池
     * @param operation 要执行的操作
     */
    public static void executeVoid(JedisPool jedisPool, java.util.function.Consumer<Jedis> operation) {
        Jedis jedis = null;
        try {
            jedis = jedisPool.getResource();
            operation.accept(jedis);
        } catch (Exception e) {
            log.error("Jedis操作失败", e);
        } finally {
            if (jedis != null) {
                jedisPool.returnResource(jedis);
            }
        }
    }
}

