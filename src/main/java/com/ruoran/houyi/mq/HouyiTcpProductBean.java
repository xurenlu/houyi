package com.ruoran.houyi.mq;

import com.aliyun.openservices.ons.api.bean.ProducerBean;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;

/**
 * 注册的tcp的生产者，用于构建完消息之后
 * @author lh
 */
@Slf4j
@Service
public class HouyiTcpProductBean {
    @Resource
    private MqConfig mqConfig;

    @Resource
    private AliyunConfig aliyunConfig;

    @Bean(initMethod = "start", destroyMethod = "shutdown")
    public ProducerBean buildProducer() {
        ProducerBean producer = new ProducerBean();
        log.error("mqConfig"+new JSONObject(mqConfig.getMqPropertie(aliyunConfig)).toString());
        producer.setProperties(mqConfig.getMqPropertie(aliyunConfig));
        return producer;
    }
}
