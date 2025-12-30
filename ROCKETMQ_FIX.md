# RocketMQ ConsumerBean 缺失问题修复

## 问题描述

应用启动时报错：
```
***************************
APPLICATION FAILED TO START
***************************

Description:

A component required a bean of type 'com.aliyun.openservices.ons.api.bean.ConsumerBean' that could not be found.

Action:

Consider defining a bean of type 'com.aliyun.openservices.ons.api.bean.ConsumerBean' in your configuration.
```

## 根本原因

项目中有以下 RocketMQ 相关类：

1. **HouyiTcpProductBean.java** - 创建了 `ProducerBean`（生产者）✅
2. **HouyiTcpRetryConsumer.java** - 需要注入 `ConsumerBean`（消费者）❌
3. **MqConfig.java** - RocketMQ 配置类

问题：**只创建了 ProducerBean，没有创建 ConsumerBean**

`HouyiTcpRetryConsumer` 类中第 38 行：
```java
@Resource
private ConsumerBean retryConsumer;  // 需要注入，但没有定义
```

## 解决方案

创建了新的配置类 `RocketMqConsumerConfig.java`，用于创建 ConsumerBean：

```java
package com.ruoran.houyi.mq;

import com.aliyun.openservices.ons.api.bean.ConsumerBean;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class RocketMqConsumerConfig {
    
    @Resource
    private MqConfig mqConfig;
    
    @Resource
    private AliyunConfig aliyunConfig;
    
    @Bean(name = "retryConsumer", initMethod = "start", destroyMethod = "shutdown")
    public ConsumerBean buildRetryConsumer() {
        log.info("创建 RocketMQ 重试消费者: topic={}, groupId={}", 
            mqConfig.getRetryTopic(), mqConfig.getRetryGroupId());
        
        ConsumerBean consumerBean = new ConsumerBean();
        consumerBean.setProperties(mqConfig.getMqProperties(aliyunConfig));
        
        return consumerBean;
    }
}
```

## 修复内容

### 新增文件
- `src/main/java/com/ruoran/houyi/mq/RocketMqConsumerConfig.java`

### 关键点
1. 创建名为 `retryConsumer` 的 ConsumerBean（与 HouyiTcpRetryConsumer 中的注入名称匹配）
2. 使用 `initMethod = "start"` 自动启动消费者
3. 使用 `destroyMethod = "shutdown"` 优雅关闭
4. 使用 MqConfig 提供的配置属性

## 部署说明

### 文件位置
- 本地: `/Users/rocky/Sites/dayu/target/houyi-0.0.1-SNAPSHOT.jar`
- 服务器: `root@106.14.70.42:/tmp/houyi-fixed.jar`

### 部署步骤

```bash
# 1. 登录服务器
ssh root@106.14.70.42

# 2. 备份当前版本
cd /home/admin/houyi
cp houyi-0.0.1-SNAPSHOT.jar houyi-0.0.1-SNAPSHOT.jar.backup-$(date +%Y%m%d-%H%M%S)

# 3. 部署新版本
mv /tmp/houyi-fixed.jar houyi-0.0.1-SNAPSHOT.jar

# 4. 确保私钥文件存在
ls -l /etc/houyi/corp-1.pem
# 如果不存在，需要创建并设置权限
# mkdir -p /etc/houyi
# vim /etc/houyi/corp-1.pem  # 粘贴私钥内容
# chmod 600 /etc/houyi/corp-1.pem

# 5. 重启应用（方式一：使用 supervisor）
supervisorctl restart houyi
supervisorctl status houyi

# 或者方式二：手动启动
pkill -f houyi-0.0.1-SNAPSHOT.jar
nohup java -Xms2g -Xmx2g -XX:+UseG1GC -XX:MaxGCPauseMillis=200 \
  -Dspring.profiles.active=prod \
  -Dspring.config.location=classpath:/application.yml,file:/home/admin/houyi/config/application.yml \
  -Dlog.path=/var/log/houyi \
  -jar houyi-0.0.1-SNAPSHOT.jar \
  >> /var/log/houyi/houyi.log 2>&1 &

# 6. 查看日志
tail -f /var/log/houyi/houyi_stdout.log
# 或
tail -f /var/log/houyi/error.2025-12-30.log
```

## 验证

启动成功后，应该能看到以下日志：

```
创建 RocketMQ 重试消费者: topic=wechat-archive-retry, groupId=wechat_msg_gid_delay
初始化 RocketMQ TCP 重试消费者: topic=wechat-archive-retry, groupId=wechat_msg_gid_delay, tag=wechat_msg
RocketMQ TCP 重试消费者初始化完成
```

## RocketMQ 配置要求

确保以下环境变量已配置（在 `/etc/houyi/.env` 或配置文件中）：

```bash
# RocketMQ 5.0 配置
ROCKETMQ_NAME_SRV_ADDR=rmq-cn-e4k4hry5b07.cn-shanghai.rmq.aliyuncs.com:8080
ROCKETMQ_TOPIC=wechat-archive-msg
ROCKETMQ_RETRY_TOPIC=wechat-archive-retry
ROCKETMQ_GROUP_ID=wechat_msg_gid_order
ROCKETMQ_RETRY_GROUP_ID=wechat_msg_gid_delay
ROCKETMQ_TAG=wechat_msg
ROCKETMQ_NAMESPACE=
ROCKETMQ_PUBLIC_ENDPOINT=ep-uf6ia8cc038842986ac2.epsrv-uf6lsm3b2e4jyv0gnsrc.cn-shanghai.privatelink.aliyuncs.com:8080
ROCKETMQ_RETRY_DELAY_MS=30000

# 阿里云 AccessKey（RocketMQ 需要）
ALIYUN_ACCESS_KEY=your_access_key_here
ALIYUN_ACCESS_SECRET=your_access_secret_here
```

## 相关文件

- `src/main/java/com/ruoran/houyi/mq/RocketMqConsumerConfig.java` - 新增的消费者配置
- `src/main/java/com/ruoran/houyi/mq/HouyiTcpProductBean.java` - 生产者配置
- `src/main/java/com/ruoran/houyi/mq/HouyiTcpRetryConsumer.java` - 重试消费者
- `src/main/java/com/ruoran/houyi/mq/MqConfig.java` - RocketMQ 配置类
- `src/main/java/com/ruoran/houyi/mq/AliyunConfig.java` - 阿里云配置类

## 注意事项

1. **开发环境跳过**：`HouyiTcpRetryConsumer` 在开发环境（dev）会跳过初始化
2. **生产环境启用**：只有在生产环境（prod）才会启动重试消费者
3. **私钥文件必需**：确保 `/etc/houyi/corp-1.pem` 存在且可读
4. **RocketMQ 配置必需**：所有 RocketMQ 相关的环境变量都必须配置

## 修复时间

- 2025-12-30 20:17
- 编译文件数：58 个（新增 1 个）
- JAR 包大小：152MB

