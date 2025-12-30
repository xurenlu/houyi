# Redis MQ 快速开始

## 已完成的改动

✅ **已切换到 Redis MQ**  
✅ **延迟队列使用数据库辅助实现**  
✅ **配置已更新，Redis MQ 默认启用**

## 快速部署

### 1. 数据库表

**自动建表（开发/测试环境）**

Spring Boot 启动时会自动创建 `delay_message` 表，无需手动执行 SQL 脚本。

**手动建表（生产环境推荐）**

为了确保表结构和索引完全符合预期，建议在生产环境手动执行：

```bash
mysql -u用户名 -p数据库名 < sql/delay_message_table.sql
```

### 2. 配置环境变量

确保以下环境变量已配置：

```bash
# Redis
export REDIS_HOST=your-redis-host
export REDIS_PORT=6379
export REDIS_PASSWORD=your-password

# MySQL
export MYSQL_HOST=your-mysql-host
export MYSQL_PORT=3306
export MYSQL_DATABASE=your-database
export MYSQL_USERNAME=your-username
export MYSQL_PASSWORD=your-password
```

### 3. 编译打包

```bash
mvn clean package -DskipTests
```

### 4. 部署到服务器

#### 方式一：使用部署脚本

```bash
./deploy.sh <服务器地址> [用户名]
```

#### 方式二：手动部署

```bash
# 上传文件
scp target/houyi-0.0.1-SNAPSHOT.jar user@server:/opt/houyi/

# SSH 登录服务器
ssh user@server

# 运行应用
cd /opt/houyi
java -jar -Dspring.profiles.active=prod houyi-0.0.1-SNAPSHOT.jar
```

## 核心改动

### 1. 延迟消息存储

- **之前**：使用 Redis Sorted Set 存储延迟消息
- **现在**：使用数据库 `delay_message` 表存储延迟消息

**优势：**
- ✅ 数据持久化，应用重启不丢失
- ✅ 不受 Redis 内存限制
- ✅ 支持更复杂的查询和统计
- ✅ 更好的可靠性

### 2. 延迟消息处理

- **扫描机制**：每秒扫描一次数据库中的到期消息
- **批量处理**：每次处理一批消息（默认 10 条）
- **自动清理**：每天凌晨 2 点清理 7 天前的已投递消息

### 3. 新增组件

- `DelayMessage` - 延迟消息实体类
- `DelayMessageRepo` - 延迟消息 Repository
- `RedisDelayMessageProcessor` - 延迟消息处理器（已更新为从数据库读取）

## 配置说明

当前配置（`application.yml`）：

```yaml
redis:
  mq:
    enabled: true  # Redis MQ 已启用
    topic: mq:topic:main
    retry-topic: mq:topic:retry
    tag: default
    retry-delay-ms: 30000
    batch-size: 10
    delay-scan-interval-ms: 1000
```

## 监控和日志

### 查看应用日志

```bash
# 如果使用 systemd
sudo journalctl -u houyi -f

# 或查看日志文件
tail -f logs/application.log
```

### 查看监控指标

```bash
curl http://your-server:8080/houyi-eye/actuator/prometheus
```

### 检查数据库

```sql
-- 查看待投递的消息
SELECT COUNT(*) FROM delay_message WHERE status = 0;

-- 查看已投递的消息
SELECT COUNT(*) FROM delay_message WHERE status = 1;

-- 查看投递失败的消息
SELECT COUNT(*) FROM delay_message WHERE status = 2;
```

## 故障排查

### 延迟消息未投递

1. 检查数据库连接
2. 查看 `delay_message` 表中是否有待投递的消息
3. 检查应用日志中的错误信息
4. 确认 `RedisDelayMessageProcessor` 是否正常运行

### 消息发送失败

1. 检查 Redis 连接
2. 检查 Redis Stream 是否正常
3. 查看应用日志中的错误信息

## 回滚到 RocketMQ

如果需要回滚：

1. 修改配置：
   ```yaml
   redis:
     mq:
       enabled: false
   ```

2. 配置 RocketMQ 环境变量

3. 重启应用

## 更多信息

- 详细部署指南：`DEPLOY_REDIS_MQ.md`
- Redis MQ 使用指南：`REDIS_MQ_GUIDE.md`

