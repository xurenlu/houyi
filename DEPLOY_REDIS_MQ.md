# Redis MQ 部署指南

## 概述

项目已切换到 **Redis MQ**，并使用**数据库辅助实现延迟队列**，提供更可靠的延迟消息处理能力。

## 部署步骤

### 1. 数据库准备

**方式一：自动建表（推荐用于开发/测试环境）**

Spring Boot 启动时会自动创建表（因为配置了 `spring.jpa.hibernate.ddl-auto: update`），无需手动执行 SQL 脚本。

**方式二：手动执行 SQL 脚本（推荐用于生产环境）**

为了确保表结构和索引完全符合预期，建议在生产环境手动执行 SQL 脚本：

```bash
mysql -u用户名 -p数据库名 < sql/delay_message_table.sql
```

或者手动执行：

```sql
CREATE TABLE IF NOT EXISTS `delay_message` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `topic` VARCHAR(128) NOT NULL COMMENT '目标 Topic',
  `message_body` TEXT NOT NULL COMMENT '消息内容（JSON 格式）',
  `message_key` VARCHAR(128) DEFAULT NULL COMMENT '消息 Key',
  `tag` VARCHAR(64) DEFAULT NULL COMMENT '消息标签',
  `sharding_key` VARCHAR(128) DEFAULT NULL COMMENT '分片 Key',
  `deliver_time` BIGINT NOT NULL COMMENT '投递时间戳（毫秒）',
  `status` TINYINT DEFAULT 0 COMMENT '消息状态：0-待投递，1-已投递，2-投递失败',
  `retry_count` INT DEFAULT 0 COMMENT '重试次数',
  `create_at` BIGINT NOT NULL COMMENT '创建时间（毫秒）',
  `deliver_at` BIGINT DEFAULT NULL COMMENT '投递时间（毫秒）',
  `error_msg` TEXT DEFAULT NULL COMMENT '错误信息',
  PRIMARY KEY (`id`),
  KEY `idx_deliver_time` (`deliver_time`, `status`),
  KEY `idx_status` (`status`),
  KEY `idx_message_key` (`message_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='延迟消息表';
```

### 2. 配置检查

确认 `application.yml` 中 Redis MQ 已启用：

```yaml
redis:
  mq:
    enabled: true  # 已设置为 true
```

### 3. 环境变量配置

确保以下环境变量已配置：

```bash
# Redis 配置
export REDIS_HOST=your-redis-host
export REDIS_PORT=6379
export REDIS_PASSWORD=your-redis-password
export REDIS_DATABASE=0

# MySQL 配置
export MYSQL_HOST=your-mysql-host
export MYSQL_PORT=3306
export MYSQL_DATABASE=your-database
export MYSQL_USERNAME=your-username
export MYSQL_PASSWORD=your-password

# Redis MQ 配置（可选，使用默认值）
export REDIS_MQ_ENABLED=true
export REDIS_MQ_TOPIC=mq:topic:main
export REDIS_MQ_RETRY_TOPIC=mq:topic:retry
export REDIS_MQ_TAG=default
export REDIS_MQ_RETRY_DELAY_MS=30000
```

### 4. 编译打包

```bash
# 使用 Maven 打包
mvn clean package -DskipTests

# 或使用 Spring Boot Maven 插件
mvn spring-boot:build-image
```

### 5. 部署到服务器

#### 方式一：直接运行 JAR

```bash
# 上传 JAR 文件到服务器
scp target/houyi-0.0.1-SNAPSHOT.jar user@server:/path/to/app/

# SSH 登录服务器
ssh user@server

# 运行应用
cd /path/to/app
java -jar -Dspring.profiles.active=prod houyi-0.0.1-SNAPSHOT.jar
```

#### 方式二：使用 systemd 服务

创建服务文件 `/etc/systemd/system/houyi.service`：

```ini
[Unit]
Description=Houyi Application
After=network.target mysql.service redis.service

[Service]
Type=simple
User=your-user
WorkingDirectory=/path/to/app
Environment="JAVA_HOME=/usr/lib/jvm/java-21"
Environment="SPRING_PROFILES_ACTIVE=prod"
Environment="REDIS_MQ_ENABLED=true"
ExecStart=/usr/bin/java -jar /path/to/app/houyi-0.0.1-SNAPSHOT.jar
Restart=always
RestartSec=10

[Install]
WantedBy=multi-user.target
```

启动服务：

```bash
sudo systemctl daemon-reload
sudo systemctl enable houyi
sudo systemctl start houyi
sudo systemctl status houyi
```

### 6. 验证部署

#### 检查应用日志

```bash
# 查看日志
tail -f logs/application.log

# 或使用 systemd
sudo journalctl -u houyi -f
```

#### 检查关键组件

1. **Redis MQ Producer 是否启动**
   - 日志中应看到：`Redis 消息队列生产者已初始化`

2. **Redis MQ Consumer 是否启动**
   - 日志中应看到：`Redis 重试消费者启动成功`

3. **延迟消息处理器是否启动**
   - 日志中应看到：`Redis 延迟消息处理器已启动`

4. **数据库连接是否正常**
   - 检查应用健康检查端点：`http://your-server:8080/houyi-eye/health`

#### 检查数据库表

```sql
-- 查看延迟消息表结构
DESC delay_message;

-- 查看待投递的消息数量
SELECT COUNT(*) FROM delay_message WHERE status = 0;

-- 查看已投递的消息数量
SELECT COUNT(*) FROM delay_message WHERE status = 1;
```

## 架构说明

### 消息流程

1. **普通消息**：
   ```
   Producer → Redis Stream → Consumer
   ```

2. **延迟消息**：
   ```
   Producer → 数据库 delay_message 表
      ↓ (定时扫描)
   Redis Stream (retry topic) → Consumer
   ```

### 延迟消息处理机制

1. **存储**：延迟消息存储在数据库 `delay_message` 表中
2. **扫描**：`RedisDelayMessageProcessor` 每秒扫描一次到期消息
3. **投递**：到期的消息投递到 Redis Stream
4. **清理**：每天凌晨 2 点自动清理 7 天前的已投递消息

## 监控指标

应用会记录以下监控指标（可通过 Prometheus 端点查看）：

- `houyi_push_cost{type="redis"}` - 消息发送耗时
- `houyi_pushed_msg{service="redis",type="normal|delay|error"}` - 消息发送计数
- `houyi_retry_msg{result="success|error|invalid|..."}` - 重试消息处理结果

访问监控端点：

```bash
curl http://your-server:8080/houyi-eye/actuator/prometheus
```

## 故障排查

### 1. 延迟消息未投递

**检查项：**
- 数据库连接是否正常
- `delay_message` 表中是否有待投递的消息
- `RedisDelayMessageProcessor` 是否正常运行
- 查看应用日志中的错误信息

**SQL 查询：**
```sql
-- 查看待投递的消息
SELECT * FROM delay_message 
WHERE status = 0 AND deliver_time <= UNIX_TIMESTAMP(NOW()) * 1000
ORDER BY deliver_time ASC
LIMIT 10;

-- 查看投递失败的消息
SELECT * FROM delay_message 
WHERE status = 2
ORDER BY create_at DESC
LIMIT 10;
```

### 2. Redis 连接失败

**检查项：**
- Redis 服务是否运行
- Redis 连接配置是否正确
- 网络连接是否正常

**测试 Redis 连接：**
```bash
redis-cli -h your-redis-host -p 6379 -a your-password ping
```

### 3. 数据库连接失败

**检查项：**
- MySQL 服务是否运行
- 数据库连接配置是否正确
- 数据库用户权限是否足够

**测试数据库连接：**
```bash
mysql -h your-mysql-host -u your-username -p your-database
```

## 性能优化建议

### 1. 数据库索引

确保以下索引已创建（SQL 脚本中已包含）：
- `idx_deliver_time` - 用于快速查询到期消息
- `idx_status` - 用于按状态查询
- `idx_message_key` - 用于按消息 Key 查询

### 2. 批量处理

调整批量处理大小（默认 10 条）：

```yaml
redis:
  mq:
    batch-size: 50  # 根据实际情况调整
```

### 3. 扫描间隔

调整延迟消息扫描间隔（默认 1 秒）：

```yaml
redis:
  mq:
    delay-scan-interval-ms: 500  # 更频繁的扫描，但会增加数据库压力
```

### 4. 定期清理

调整清理策略（默认保留 7 天）：

修改 `RedisDelayMessageProcessor.cleanDeliveredMessages()` 方法中的保留天数。

## 回滚方案

如果需要回滚到 RocketMQ：

1. 修改配置：
   ```yaml
   redis:
     mq:
       enabled: false
   ```

2. 配置 RocketMQ 环境变量

3. 重启应用

## 注意事项

1. **数据持久化**：延迟消息存储在数据库中，即使应用重启也不会丢失
2. **消息顺序**：延迟消息按 `deliver_time` 排序投递
3. **重复投递**：应用重启后，未投递的延迟消息会继续处理
4. **清理策略**：已投递的消息会在 7 天后自动清理，可根据需要调整

## 联系支持

如遇到问题，请查看：
- 应用日志：`logs/application.log`
- 监控指标：`/houyi-eye/actuator/prometheus`
- 健康检查：`/houyi-eye/actuator/health`

