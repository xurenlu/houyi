# Redis 消息队列使用指南

## 概述

本项目现在支持使用 **Redis** 替代 **RocketMQ** 作为消息队列。Redis 消息队列使用 **Redis Streams** 实现，支持消息持久化、消费者组、ACK 确认等功能。

## 功能对比

| 功能 | RocketMQ | Redis MQ |
|------|----------|----------|
| 普通消息 | ✅ | ✅ |
| 延迟消息 | ✅ | ✅（通过 Sorted Set 实现） |
| 消息标签过滤 | ✅ | ✅ |
| 消费者组 | ✅ | ✅ |
| 消息 ACK | ✅ | ✅ |
| 消息持久化 | ✅ | ✅ |
| 消息轨迹 | ✅ | ❌ |

## 架构设计

### 1. 普通消息流程

```
Producer → Redis Stream (topic) → Consumer
```

### 2. 延迟消息流程

```
Producer → Redis Sorted Set (delay queue) 
    ↓ (定时扫描)
Redis Stream (retry topic) → Consumer
```

## 配置说明

### 启用 Redis MQ

在 `application.yml` 或环境变量中设置：

```yaml
redis:
  mq:
    enabled: true  # 设置为 true 启用 Redis MQ
```

### 配置项说明

| 配置项 | 说明 | 默认值 |
|--------|------|--------|
| `redis.mq.enabled` | 是否启用 Redis MQ | `false` |
| `redis.mq.topic` | 主消息队列 Stream Key | `mq:topic:main` |
| `redis.mq.retry-topic` | 重试队列 Stream Key | `mq:topic:retry` |
| `redis.mq.delay-queue-key` | 延迟消息 Sorted Set Key | `mq:delay:queue` |
| `redis.mq.consumer-group` | 消费者组名称 | `houyi-consumer-group` |
| `redis.mq.retry-consumer-group` | 重试消费者组名称 | `houyi-retry-consumer-group` |
| `redis.mq.tag` | 消息标签 | `default` |
| `redis.mq.retry-delay-ms` | 重试延迟时间（毫秒） | `30000` |
| `redis.mq.batch-size` | 消费者批处理大小 | `10` |
| `redis.mq.poll-interval-ms` | 消费者轮询间隔（毫秒） | `1000` |
| `redis.mq.delay-scan-interval-ms` | 延迟消息扫描间隔（毫秒） | `1000` |

## 使用方式

### 代码层面

项目已经实现了适配器模式，**无需修改业务代码**。只需通过配置切换即可：

```java
// 业务代码保持不变
@Resource
private MessageProducerAdapter messageProducer;

// 发送普通消息
messageProducer.send(message, messageKey);

// 发送延迟消息
messageProducer.sendDelayMessage(message, messageKey, delayTimeMs);
```

### 环境变量切换

**使用 Redis MQ：**
```bash
export REDIS_MQ_ENABLED=true
```

**使用 RocketMQ（默认）：**
```bash
export REDIS_MQ_ENABLED=false
# 或不设置该变量
```

## 核心组件

### 1. RedisMqProducer

Redis 消息队列生产者，实现 `MessageProducerInterface` 接口。

**功能：**
- 发送普通消息到 Redis Stream
- 发送延迟消息到 Redis Sorted Set（延迟队列）

### 2. RedisMqConsumer

Redis 消息队列消费者，消费重试队列中的消息。

**功能：**
- 从 Redis Stream 消费消息
- 支持消费者组
- 支持消息 ACK
- 支持 Tag 过滤

### 3. RedisDelayMessageProcessor

延迟消息处理器，定期扫描延迟队列，将到期的消息投递到目标 Stream。

**功能：**
- 定时扫描延迟队列（Sorted Set）
- 将到期的消息投递到目标 Stream
- 支持任意时间延迟

### 4. MessageProducerAdapter

消息生产者适配器，根据配置自动选择使用 RocketMQ 或 Redis。

## Redis 数据结构

### 1. Stream（消息队列）

```
Key: mq:topic:main 或 mq:topic:retry
Type: Stream
Fields:
  - body: 消息内容
  - key: 消息 Key
  - tag: 消息标签
  - shardingKey: 分片 Key（可选）
```

### 2. Sorted Set（延迟队列）

```
Key: mq:delay:queue
Type: Sorted Set
Score: 延迟时间戳（毫秒）
Value: JSON 格式的延迟消息
{
  "id": "消息ID",
  "topic": "目标 Topic",
  "body": "消息内容",
  "key": "消息 Key",
  "tag": "消息标签",
  "deliverTime": 延迟时间戳
}
```

## 性能考虑

### 优势

1. **无需额外部署**：使用已有的 Redis 服务
2. **低延迟**：Redis 内存操作，延迟低
3. **简单易用**：无需复杂的配置和运维

### 限制

1. **消息容量**：受 Redis 内存限制
2. **持久化**：需要配置 Redis 持久化（AOF/RDB）
3. **高可用**：需要 Redis 集群或哨兵模式
4. **延迟精度**：取决于扫描间隔（默认 1 秒）

## 监控指标

Redis MQ 会记录以下监控指标：

- `houyi_push_cost{type="redis"}` - 消息发送耗时
- `houyi_pushed_msg{service="redis",type="normal|delay|error"}` - 消息发送计数
- `houyi_retry_msg{result="success|error|invalid|..."}` - 重试消息处理结果
- `houyi_shard_key{source="from|random"}` - ShardingKey 来源统计

## 迁移建议

### 从 RocketMQ 迁移到 Redis MQ

1. **测试环境验证**
   ```bash
   export REDIS_MQ_ENABLED=true
   # 运行测试，验证功能正常
   ```

2. **生产环境切换**
   - 确保 Redis 有足够的内存
   - 配置 Redis 持久化
   - 设置 Redis 高可用（如需要）
   - 切换配置并重启应用

3. **监控观察**
   - 观察消息发送成功率
   - 观察延迟消息处理情况
   - 观察 Redis 内存使用情况

### 注意事项

1. **消息不兼容**：RocketMQ 和 Redis MQ 的消息格式不同，不能混用
2. **消费者组**：切换时需要重新创建消费者组
3. **延迟消息**：Redis 的延迟消息精度取决于扫描间隔
4. **数据清理**：定期清理已消费的消息，避免 Redis 内存占用过大

## 故障排查

### 1. 消息发送失败

- 检查 Redis 连接是否正常
- 检查 Redis 内存是否充足
- 查看日志中的错误信息

### 2. 延迟消息未投递

- 检查 `RedisDelayMessageProcessor` 是否正常运行
- 检查延迟队列中是否有消息
- 检查扫描间隔配置是否合理

### 3. 消费者未消费消息

- 检查消费者组是否创建成功
- 检查 Stream 中是否有消息
- 检查 Tag 过滤配置是否正确

## 总结

Redis MQ 是一个轻量级的消息队列解决方案，适合：
- 消息量不是特别大的场景
- 希望减少外部依赖的场景
- 对延迟精度要求不高的场景

如果消息量很大或对可靠性要求极高，建议继续使用 RocketMQ。

