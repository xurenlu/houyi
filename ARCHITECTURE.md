# 后羿（Houyi）系统架构文档

## 📋 系统概述

后羿（Houyi）是一个企业微信会话存档系统，用于自动拉取、处理、存储企业微信的会话数据。

### 核心功能

1. **会话数据拉取**: 从企业微信服务器拉取会话记录
2. **媒体文件下载**: 下载图片、视频、文件等媒体内容
3. **数据持久化**: 存储到 MySQL 数据库
4. **文件存储**: 上传到阿里云 OSS
5. **消息分发**: 通过 RocketMQ 和 MNS 分发给下游系统
6. **失败重试**: 自动重试下载失败的文件

---

## 🏗️ 系统架构图

```
┌─────────────────────────────────────────────────────────────────┐
│                        企业微信官方服务器                          │
│                    (腾讯 WeWork API Server)                      │
└────────────────────────┬────────────────────────────────────────┘
                         │ 企业微信 SDK (JNI)
                         │ libWeWorkFinanceSdk_Java.so
                         ↓
┌─────────────────────────────────────────────────────────────────┐
│                      后羿应用服务器 (Houyi)                        │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │  Start.java (启动入口)                                     │  │
│  │  - 初始化企业微信 SDK                                       │  │
│  │  - 为每个企业创建 Message 线程                              │  │
│  └────────────┬─────────────────────────────────────────────┘  │
│               ↓                                                 │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │  Message.java (消息拉取线程)                               │  │
│  │  - 定时拉取会话数据                                         │  │
│  │  - 解密消息内容                                             │  │
│  │  - 分发给 MsgHandler                                       │  │
│  └────────────┬─────────────────────────────────────────────┘  │
│               ↓                                                 │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │  MsgHandler.java (消息处理器)                              │  │
│  │  - 判断消息类型                                             │  │
│  │  - 调用 MediaDownloader 下载文件                           │  │
│  │  - MD5 校验                                                │  │
│  │  - 上传到 OSS                                              │  │
│  │  - 发送到 RocketMQ                                         │  │
│  └────────────┬─────────────────────────────────────────────┘  │
│               ↓                                                 │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │  MediaDownloader.java (媒体下载器)                         │  │
│  │  - 分片下载大文件                                           │  │
│  │  - MD5 校验                                                │  │
│  │  - 异常处理和重试                                           │  │
│  └──────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────┘
                         │
        ┌────────────────┼────────────────┐
        ↓                ↓                ↓
┌──────────────┐  ┌──────────────┐  ┌──────────────┐
│   MySQL      │  │   Redis      │  │  阿里云 OSS   │
│  (持久化)     │  │  (缓存)      │  │  (文件存储)   │
└──────────────┘  └──────────────┘  └──────────────┘
        ↓
┌─────────────────────────────────────────────────────────────────┐
│                     消息队列 (Message Queue)                      │
│  ┌──────────────────────┐      ┌──────────────────────┐        │
│  │  RocketMQ (TCP)      │      │  RocketMQ (HTTP)     │        │
│  │  主消息队列            │      │  重试队列             │        │
│  │  Topic: wechat-      │      │  Topic: msg_center   │        │
│  │  archive-msg-common  │      │                      │        │
│  └──────────┬───────────┘      └──────────┬───────────┘        │
│             │                             │                     │
│             │                             ↓                     │
│             │                  ┌──────────────────────┐        │
│             │                  │ HouyiMqHttpConsumer  │        │
│             │                  │ (重试消费者)          │        │
│             │                  └──────────┬───────────┘        │
│             │                             │                     │
│             │                             ↓                     │
│             │                  ┌──────────────────────┐        │
│             │                  │ DownloadThreadKeeper │        │
│             │                  │ (重新下载)            │        │
│             │                  └──────────────────────┘        │
│  ┌──────────┴───────────┐                                      │
│  │  阿里云 MNS           │                                      │
│  │  Queue: houyi-msgs   │                                      │
│  └──────────┬───────────┘                                      │
└─────────────┼────────────────────────────────────────────────────┘
              ↓
┌─────────────────────────────────────────────────────────────────┐
│                        下游消费系统                               │
│                   (External Consumer Systems)                    │
└─────────────────────────────────────────────────────────────────┘
```

---

## 🔄 数据流转详解

### 1. 正常流程

```
1. Start.java 启动
   ↓
2. 读取企业微信配置 (wework-corps.yml)
   ↓
3. 为每个企业创建 Message 线程
   ↓
4. Message 线程定时拉取会话数据
   ├─ Finance.GetChatData() → 企业微信服务器
   ├─ 解密消息 (RSA + AES)
   └─ 判断消息类型
      ├─ 文本消息 → 直接入库 + 发送 MQ
      └─ 媒体消息 → 提交到下载线程池
         ↓
5. DownloadThreadKeeper 执行下载任务
   ↓
6. MsgHandler.simpleDownMedia()
   ├─ MediaDownloader.download()
   │  ├─ Finance.GetMediaData() → 企业微信服务器
   │  ├─ 分片下载（每次 512KB）
   │  └─ 写入本地临时文件
   ├─ MD5 校验
   ├─ AMR 转 MP3（语音消息）
   ├─ 上传到阿里云 OSS
   │  └─ OssUtil.upload()
   ├─ 保存到 MySQL
   │  └─ OriginalMsg 表
   └─ 发送到消息队列
      ├─ RocketMQ (TCP) → 下游系统
      └─ MNS → 备份队列
```

### 2. 失败重试流程

```
下载失败 / MD5 校验失败 / OSS 上传失败
   ↓
RetryUtil.sendRetryMessage()
   ↓
HouyiMqProducer.send()
   ├─ Topic: msg_center
   ├─ Tag: prod/dev
   ├─ 延迟: 30 秒
   └─ tryCount++
      ↓
HouyiMqHttpConsumer 消费
   ├─ 检查 tryCount (最多 16 次)
   ├─ 检查数据库是否已有 ossPath
   └─ 提交到 DownloadThreadKeeper
      ↓
重新执行下载流程
   ↓
成功 → 正常流程
失败 → 继续重试（直到 16 次）
```

---

## 🗄️ 数据库设计

### 核心表结构

#### 1. corplist (企业信息表)

```sql
CREATE TABLE corplist (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    corpid VARCHAR(255) NOT NULL,           -- 企业微信 ID
    corpname VARCHAR(255),                  -- 企业名称
    secret VARCHAR(255),                    -- 企业微信 Secret
    prikey TEXT,                            -- RSA 私钥
    status BIGINT DEFAULT 1,                -- 状态 (1=启用)
    UNIQUE KEY uk_corpid (corpid)
);
```

#### 2. original_msg (原始消息表)

```sql
CREATE TABLE original_msg (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    corp_id VARCHAR(255),                   -- 企业微信 ID
    msg_id VARCHAR(255),                    -- 消息 ID
    seq BIGINT,                             -- 消息序列号
    msgtype VARCHAR(50),                    -- 消息类型
    msgtime BIGINT,                         -- 消息时间戳
    from_user VARCHAR(255),                 -- 发送者
    to_list TEXT,                           -- 接收者列表
    roomid VARCHAR(255),                    -- 群聊 ID
    oss_path TEXT,                          -- OSS 存储路径
    md5sum VARCHAR(255),                    -- 文件 MD5
    filesize BIGINT,                        -- 文件大小
    push_at BIGINT,                         -- 推送时间
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_corp_msg_seq (corp_id, msg_id, seq),
    INDEX idx_push_at (push_at),
    INDEX idx_msgtype (msgtype)
);
```

#### 3. md5_index (MD5 索引表)

```sql
CREATE TABLE md5_index (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    md5sum VARCHAR(255) NOT NULL,           -- 文件 MD5
    oss_path TEXT,                          -- OSS 存储路径
    filesize BIGINT,                        -- 文件大小
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_md5 (md5sum)
);
```

**用途**: 文件去重，相同 MD5 的文件只存储一次

---

## 🧵 线程池配置

### 1. Download Thread Pool (下载线程池)

- **Core Size**: 8
- **Max Size**: 100
- **Queue Capacity**: 10000
- **用途**: 并发下载媒体文件

### 2. OSS Thread Pool (OSS 上传线程池)

- **Core Size**: 4
- **Max Size**: 60
- **Queue Capacity**: 10000
- **用途**: 并发上传文件到 OSS

### 3. MNS Thread Pool (MNS 推送线程池)

- **Core Size**: 16
- **Max Size**: 64
- **Queue Capacity**: 1280
- **用途**: 并发推送消息到 MNS

---

## 🔌 外部依赖

### 1. 企业微信 SDK

- **类型**: JNI (Java Native Interface)
- **库文件**: `libWeWorkFinanceSdk_Java.so` (Linux)
- **协议**: 直连企业微信服务器（无代理）
- **限制**: 需要配置 IP 白名单

### 2. 阿里云 OSS

- **用途**: 存储媒体文件
- **Endpoint**: 
  - 公网: `oss-cn-shanghai.aliyuncs.com`
  - 内网: `oss-cn-shanghai-internal.aliyuncs.com`
- **Bucket**: `wechat-monitoring`

### 3. 阿里云 RocketMQ

#### TCP 实例（主消息队列）
- **Instance ID**: `MQ_INST_1689796288110055_BXrMWl6w`
- **Topic**: `wechat-archive-msg-common`
- **Tag**: `ChatDataJava`
- **消息类型**: 顺序消息

#### HTTP 实例（重试队列）
- **Instance ID**: `MQ_INST_1689796288110055_BXkckLjP`
- **Topic**: `msg_center`
- **Tag**: `prod` / `dev`
- **消息类型**: 延迟消息（30 秒）

### 4. 阿里云 MNS

- **Queue Name**: `houyi-msgs`
- **用途**: 备份消息队列

### 5. MySQL

- **Database**: `wechat-archive`
- **版本**: 8.0+
- **用途**: 持久化存储

### 6. Redis

- **版本**: 5.0+
- **用途**: 
  - 缓存 MD5 索引
  - 缓存最后拉取的 seq

---

## 📦 核心组件

### 启动组件

| 组件 | 类型 | 说明 |
|------|------|------|
| `HouyiApplication` | Spring Boot 主类 | 应用入口 |
| `Start` | CommandLineRunner | 启动时初始化 SDK 和线程 |
| `CorpConfigService` | Service | 加载企业配置到数据库 |

### 消息处理组件

| 组件 | 类型 | 说明 |
|------|------|------|
| `Message` | Thread | 拉取会话数据线程 |
| `MsgHandler` | Component | 消息处理器 |
| `MixedHandler` | Component | 混合消息处理器 |
| `MediaDownloader` | Component | 媒体文件下载器 |

### 线程池组件

| 组件 | 类型 | 说明 |
|------|------|------|
| `DownloadThreadKeeper` | Component | 下载线程池管理 |
| `OssThreadPool` | Component | OSS 上传线程池 |
| `ThreadPoolConfig` | Configuration | 线程池配置 |

### 消息队列组件

| 组件 | 类型 | 说明 |
|------|------|------|
| `HouyiTcpConstructionMessageProduct` | Component | TCP 生产者（主队列） |
| `HouyiHttpConstructionMessageProduct` | Service | HTTP 生产者 |
| `HouyiMqProducer` | Service | HTTP 生产者（重试队列） |
| `HouyiMqHttpConsumer` | Thread | HTTP 消费者（重试队列） |

### 工具组件

| 组件 | 类型 | 说明 |
|------|------|------|
| `OssUtil` | Component | OSS 上传工具 |
| `JedisUtil` | Component | Redis 工具 |
| `FileUtil` | Utility | 文件操作工具 |
| `DateUtil` | Utility | 日期格式化工具 |
| `RetryUtil` | Utility | 重试逻辑工具 |
| `HttpClientUtil` | Utility | HTTP 客户端工具 |

---

## 🔐 安全机制

### 1. 环境变量管理

所有敏感信息通过环境变量配置：
- 阿里云 AccessKey/Secret
- 数据库密码
- Redis 密码
- 企业微信 Secret 和私钥

### 2. 数据加密

- **传输加密**: 企业微信 SDK 使用 HTTPS
- **消息解密**: RSA + AES 混合加密
- **私钥存储**: 数据库加密存储（建议）

### 3. 访问控制

- **IP 白名单**: 企业微信会话存档需要配置 IP 白名单
- **AK/SK 权限**: 阿里云 AK 最小权限原则

---

## 📊 监控指标

### Prometheus 指标

系统暴露以下监控指标：

| 指标名称 | 类型 | 说明 |
|---------|------|------|
| `houyi_pushed_msg_total` | Counter | 推送消息总数 |
| `houyi_push_cost_seconds` | Summary | 推送耗时 |
| `houyi_shard_key_total` | Counter | ShardingKey 使用统计 |
| `houyi_mns_real_pushed` | Counter | MNS 实际推送数 |

### Actuator 端点

- `/houyi-eye/health` - 健康检查
- `/houyi-eye/metrics` - 应用指标
- `/houyi-eye/prometheus` - Prometheus 格式指标
- `/houyi-eye/info` - 应用信息

---

## 🚀 部署架构

### 单机部署

```
┌─────────────────────────────────────┐
│     后羿应用服务器 (Houyi Server)     │
│  ┌─────────────────────────────┐   │
│  │  JVM (Java 21)              │   │
│  │  - Spring Boot 3.2.11       │   │
│  │  - 8 个 Message 线程         │   │
│  │  - 100 个下载线程            │   │
│  │  - 60 个 OSS 上传线程        │   │
│  └─────────────────────────────┘   │
└─────────────────────────────────────┘
         ↓           ↓           ↓
    ┌────────┐  ┌────────┐  ┌────────┐
    │ MySQL  │  │ Redis  │  │  OSS   │
    └────────┘  └────────┘  └────────┘
```

### 高可用部署（推荐）

```
        ┌─────────────┐
        │   Nginx     │
        │ Load Balancer│
        └──────┬──────┘
               │
      ┌────────┼────────┐
      ↓        ↓        ↓
┌──────────┐ ┌──────────┐ ┌──────────┐
│ Houyi-1  │ │ Houyi-2  │ │ Houyi-3  │
│ (主节点)  │ │ (备节点)  │ │ (备节点)  │
└──────────┘ └──────────┘ └──────────┘
      ↓        ↓        ↓
┌─────────────────────────────────┐
│     MySQL (主从复制)              │
│     Redis (哨兵模式)              │
│     OSS (自动高可用)              │
│     RocketMQ (集群模式)           │
└─────────────────────────────────┘
```

**注意**: 由于 Message 线程会定时拉取数据，多节点部署时需要：
1. 使用 Redis 分布式锁避免重复拉取
2. 或者按企业 ID 分片部署（推荐）

---

## 🔧 配置文件层级

```
application.yml (基础配置)
    ├─ application-dev.yml (开发环境)
    └─ application-prod.yml (生产环境)

wework-corps.yml (企业微信配置)

logback-spring.xml (日志配置)

.env (环境变量，不提交到 Git)
```

---

## 📚 相关文档

- [README.md](README.md) - 项目总览和快速开始
- [ROCKETMQ_GUIDE.md](ROCKETMQ_GUIDE.md) - RocketMQ 详细配置
- [WEWORK_CONFIG_GUIDE.md](WEWORK_CONFIG_GUIDE.md) - 企业微信配置
- [SECURITY.md](SECURITY.md) - 安全配置指南
- [UPGRADE_SUMMARY.md](UPGRADE_SUMMARY.md) - 技术栈升级记录
- [FINAL_REPORT.md](FINAL_REPORT.md) - 代码质量报告

---

## 🎯 性能指标

### 吞吐量

- **消息拉取**: 500 条/次，约 10 秒/次
- **文件下载**: 100 并发，平均 2 秒/文件
- **OSS 上传**: 60 并发，平均 1 秒/文件
- **消息推送**: 1000+ 条/秒

### 资源占用

- **CPU**: 4-8 核（推荐）
- **内存**: 4-8 GB（推荐）
- **磁盘**: 50 GB+（临时文件）
- **网络**: 100 Mbps+

---

## 🐛 常见问题

### 1. 消息拉取失败

**原因**: 
- IP 未加入企业微信白名单
- Secret 或私钥错误
- SDK 初始化失败

**解决**: 
- 检查企业微信后台配置
- 查看日志 `Finance.Init()` 返回值

### 2. 文件下载失败

**原因**:
- 网络不稳定
- sdkFileId 过期
- 企业微信服务器限流

**解决**:
- 自动重试机制（最多 16 次）
- 检查网络连接
- 查看重试队列消费情况

### 3. OSS 上传失败

**原因**:
- AccessKey/Secret 错误
- Bucket 不存在
- 网络问题

**解决**:
- 检查阿里云凭证
- 检查 Bucket 权限
- 使用内网 Endpoint

### 4. 消息堆积

**原因**:
- 下游消费者处理慢
- 下载线程池满
- 数据库写入慢

**解决**:
- 扩容下游消费者
- 增加线程池大小
- 优化数据库索引

---

**最后更新**: 2025-12-29  
**维护者**: Houyi Team

