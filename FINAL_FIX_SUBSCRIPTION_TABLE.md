# RocketMQ ConsumerBean subscriptionTable 问题修复

## 问题描述

第二次启动失败，报错：
```
Caused by: org.springframework.beans.factory.BeanCreationException: 
Error creating bean with name 'retryConsumer' defined in class path resource 
[com/ruoran/houyi/mq/RocketMqConsumerConfig.class]: subscriptionTable not set
```

## 根本原因

Spring Bean 的生命周期问题：

1. **Bean 创建阶段**：`RocketMqConsumerConfig.buildRetryConsumer()` 创建 ConsumerBean
2. **initMethod 执行**：如果配置了 `initMethod = "start"`，会立即调用 `start()` 方法
3. **@PostConstruct 执行**：`HouyiTcpRetryConsumer.init()` 设置 subscriptionTable

**问题**：`initMethod` 在 `@PostConstruct` **之前**执行，导致 `start()` 时 subscriptionTable 还没设置！

### Bean 生命周期顺序
```
1. 构造函数
2. 依赖注入 (@Autowired, @Resource)
3. BeanPostProcessor.postProcessBeforeInitialization
4. @PostConstruct 方法          ← subscriptionTable 在这里设置
5. InitializingBean.afterPropertiesSet
6. init-method                   ← start() 在这里调用（太早了！）
7. BeanPostProcessor.postProcessAfterInitialization
```

## 解决方案

### 方案：移除 initMethod，手动启动

1. **RocketMqConsumerConfig**：移除 `initMethod = "start"`
2. **HouyiTcpRetryConsumer**：在设置完 subscriptionTable 后手动调用 `start()`

## 修复内容

### 1. 修改 RocketMqConsumerConfig.java

```java
// 修改前
@Bean(name = "retryConsumer", initMethod = "start", destroyMethod = "shutdown")
public ConsumerBean buildRetryConsumer() {
    // ...
}

// 修改后
@Bean(name = "retryConsumer", destroyMethod = "shutdown")  // 移除 initMethod
public ConsumerBean buildRetryConsumer() {
    // ...
}
```

### 2. 修改 HouyiTcpRetryConsumer.java

```java
@PostConstruct
public void init() {
    if ("dev".equalsIgnoreCase(env)) {
        log.info("开发环境，跳过重试消费者初始化");
        return;
    }
    
    try {
        // 设置 subscriptionTable
        Map<Subscription, MessageListener> subscriptionTable = new HashMap<>();
        Subscription subscription = new Subscription();
        subscription.setTopic(mqConfig.getRetryTopic());
        subscription.setExpression(mqConfig.getTag());
        subscriptionTable.put(subscription, new RetryMessageListener());
        retryConsumer.setSubscriptionTable(subscriptionTable);
        
        // 手动启动消费者（在设置完 subscriptionTable 之后）
        retryConsumer.start();
        
        log.info("RocketMQ TCP 重试消费者初始化完成");
    } catch (Exception e) {
        log.error("RocketMQ TCP 重试消费者初始化失败: {}", e.getMessage(), e);
        throw new RuntimeException("Failed to initialize RocketMQ retry consumer", e);
    }
}
```

## 关键改进

1. ✅ **正确的启动顺序**：先设置 subscriptionTable，再调用 start()
2. ✅ **异常处理**：添加 try-catch，启动失败时有明确的错误信息
3. ✅ **优雅关闭**：保留 `destroyMethod = "shutdown"`，确保应用关闭时正确停止消费者

## 部署说明

### 文件位置
- 本地：`/Users/rocky/Sites/dayu/target/houyi-0.0.1-SNAPSHOT.jar`
- 服务器：`root@106.14.70.42:/tmp/houyi-final.jar`

### 部署步骤

```bash
# 1. 登录服务器
ssh root@106.14.70.42

# 2. 确保私钥文件存在
ls -l /etc/houyi/corp-1.pem
# 如果不存在：
# mkdir -p /etc/houyi
# vim /etc/houyi/corp-1.pem  # 粘贴私钥内容
# chmod 600 /etc/houyi/corp-1.pem

# 3. 备份并部署
cd /home/admin/houyi
cp houyi-0.0.1-SNAPSHOT.jar houyi-0.0.1-SNAPSHOT.jar.backup-$(date +%Y%m%d-%H%M%S)
mv /tmp/houyi-final.jar houyi-0.0.1-SNAPSHOT.jar

# 4. 重启应用
supervisorctl restart houyi

# 5. 查看日志（实时）
tail -f /var/log/houyi/houyi_stdout.log
```

### 验证成功的日志

启动成功后应该看到：

```
创建 RocketMQ 重试消费者 Bean: topic=wechat-archive-retry, groupId=wechat_msg_gid_delay
初始化 RocketMQ TCP 重试消费者: topic=wechat-archive-retry, groupId=wechat_msg_gid_delay, tag=wechat_msg
RocketMQ TCP 重试消费者初始化完成

  .   ____          _            __ _ _
 /\\ / ___'_ __ _ _(_)_ __  __ _ \ \ \ \
( ( )\___ | '_ | '_| | '_ \/ _` | \ \ \ \
 \\/  ___)| |_)| | | | | || (_| |  ) ) ) )
  '  |____| .__|_| |_|_| |_\__, | / / / /
 =========|_|==============|___/=/_/_/_/
 :: Spring Boot ::               (v3.2.11)

Started HouyiApplication in X.XXX seconds
```

## 修改的文件

1. `src/main/java/com/ruoran/houyi/mq/RocketMqConsumerConfig.java`
   - 移除 `initMethod = "start"`
   - 添加注释说明延迟启动的原因

2. `src/main/java/com/ruoran/houyi/mq/HouyiTcpRetryConsumer.java`
   - 在 `@PostConstruct` 方法中添加 `retryConsumer.start()`
   - 添加异常处理和错误日志

## 技术要点

### Spring Bean 生命周期
- **@PostConstruct**：在依赖注入完成后执行
- **initMethod**：在 @PostConstruct 之后执行
- **destroyMethod**：在 Bean 销毁前执行

### RocketMQ ConsumerBean 启动要求
1. 必须先设置 `properties`（连接配置）
2. 必须先设置 `subscriptionTable`（订阅信息）
3. 然后才能调用 `start()`（启动消费者）

### 为什么不能用 initMethod
因为 `initMethod` 在 `@PostConstruct` 之前执行，而 `subscriptionTable` 是在 `@PostConstruct` 中设置的，所以会导致启动失败。

## 相关文档

- `ROCKETMQ_FIX.md` - RocketMQ ConsumerBean 缺失问题的初步修复
- `DEPLOYMENT_NOTES.md` - 企业微信配置和部署说明
- `LOGGING_FIX.md` - 日志警告修复说明

## 修复时间

- 2025-12-30 20:21
- 最终版本：houyi-final.jar
- JAR 包大小：152MB

## 注意事项

1. **开发环境跳过**：在 dev 环境下不会启动 RocketMQ 消费者
2. **生产环境必需**：确保所有 RocketMQ 环境变量已配置
3. **私钥文件必需**：`/etc/houyi/corp-1.pem` 必须存在且可读
4. **优雅关闭**：应用关闭时会自动调用 `shutdown()` 停止消费者

