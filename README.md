# 企业微信会话存档系统 (Houyi)

[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.11-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)
[![Code Quality](https://img.shields.io/badge/Code%20Quality-A-success.svg)](FINAL_REPORT.md)

企业微信会话存档系统，用于拉取、存储和处理企业微信的会话数据。

## ✨ 特性

- 🚀 **现代技术栈**: Spring Boot 3.2.11 + Java 21
- 🔒 **安全可靠**: 所有敏感信息使用环境变量，防止泄露
- 📊 **高性能**: 多线程并发处理，支持大文件下载
- 💾 **多存储支持**: 阿里云 OSS + MySQL + Redis
- 🔄 **消息队列**: RocketMQ 异步处理
- 📈 **可观测性**: Prometheus + Actuator 监控
- 🎯 **高质量代码**: A 级代码质量，完善的异常处理

## 📋 技术栈

- **Java**: 21 (LTS)
- **Spring Boot**: 3.2.11
- **Spring Data JPA**: 3.x
- **数据库**: MySQL 8.x
- **缓存**: Redis (Jedis 5.1.0)
- **对象存储**: 阿里云 OSS
- **消息队列**: 阿里云 RocketMQ
- **加密**: BouncyCastle 1.77
- **监控**: Micrometer + Prometheus

## 🚀 快速开始

### 前置要求

- Java 21+
- Maven 3.6+
- MySQL 8.0+
- Redis 5.0+
- RocketMQ 5.0+

### 1. 克隆项目

```bash
git clone git@github.com:xurenlu/houyi.git
cd houyi
```

### 2. 配置环境变量

```bash
# 复制环境变量模板
cp .env.example .env

# 编辑 .env 文件，填入真实的配置
vim .env
```

**重要**: 请参考 [SECURITY.md](SECURITY.md) 了解如何安全地配置敏感信息。

### 3. 配置 Maven 镜像（可选，加速构建）

```bash
# 复制 Maven 配置示例到用户目录
cp .m2-settings-example.xml ~/.m2/settings.xml
```

### 4. 编译项目

```bash
mvn clean package -DskipTests
```

### 5. 运行项目

#### 方式 1: 使用启动脚本（推荐）

```bash
# 启动
./scripts/start.sh start

# 查看状态
./scripts/start.sh status

# 查看日志
./scripts/start.sh logs

# 停止
./scripts/start.sh stop

# 重启
./scripts/start.sh restart
```

#### 方式 2: 使用 systemd 服务（生产环境推荐）

```bash
# 安装服务
sudo ./scripts/install-service.sh

# 编辑配置
sudo vi /opt/houyi/.env

# 启动服务
sudo systemctl start houyi

# 查看状态
sudo systemctl status houyi

# 查看日志
sudo journalctl -u houyi -f

# 开机自启
sudo systemctl enable houyi
```

#### 方式 3: 直接运行 JAR

```bash
# 开发环境
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# 生产环境
export SPRING_PROFILES_ACTIVE=prod
java -jar target/houyi-0.0.1-SNAPSHOT.jar
```

## 📁 项目结构

```
houyi/
├── src/main/java/com/ruoran/houyi/
│   ├── config/              # 配置类
│   │   ├── ThreadPoolConfig.java    # 线程池配置
│   │   ├── HouyiProperties.java      # 应用配置
│   │   └── SwaggerConfig.java       # API 文档配置
│   ├── constants/           # 常量类
│   │   └── AppConstants.java
│   ├── controller/          # 控制器
│   ├── downloader/          # 下载器
│   │   └── MediaDownloader.java     # 媒体文件下载
│   ├── exception/           # 自定义异常
│   ├── model/              # 数据模型
│   ├── mq/                 # 消息队列
│   ├── repo/               # 数据仓库
│   ├── service/            # 业务服务
│   ├── sync/               # 同步服务
│   └── utils/              # 工具类
│       ├── DateUtil.java           # 日期工具
│       ├── FileUtil.java           # 文件工具
│       ├── JedisUtil.java          # Redis 工具
│       └── RetryUtil.java          # 重试工具
├── src/main/resources/
│   ├── application.yml              # 主配置文件
│   ├── application-dev.yml          # 开发环境配置
│   ├── application-prod.yml         # 生产环境配置
│   └── logback-spring.xml           # 日志配置
├── .env.example            # 环境变量模板
├── SECURITY.md            # 安全配置指南
├── UPGRADE_SUMMARY.md     # 升级总结
└── FINAL_REPORT.md        # 最终优化报告
```

## 🔧 配置说明

### 环境变量

所有敏感信息都通过环境变量配置，主要包括：

- **阿里云 OSS**: `ALIYUN_OSS_ACCESS_KEY`, `ALIYUN_OSS_ACCESS_SECRET`
- **数据库**: `MYSQL_HOST`, `MYSQL_USERNAME`, `MYSQL_PASSWORD`
- **Redis**: `REDIS_HOST`, `REDIS_PASSWORD`
- **RocketMQ**: `ROCKETMQ_NAME_SRV_ADDR`, `ROCKETMQ_GROUP_ID`

详细配置请参考 [.env.example](.env.example) 和 [SECURITY.md](SECURITY.md)。

### 线程池配置

可以通过环境变量或配置文件调整线程池参数：

```yaml
thread-pool:
  download:
    core-size: 8
    max-size: 100
    queue-capacity: 10000
  oss:
    core-size: 4
    max-size: 60
    queue-capacity: 10000
```

## 📊 监控

### Actuator 端点

访问 `http://localhost:8080/houyi-eye` 查看所有可用端点：

- `/houyi-eye/health` - 健康检查
- `/houyi-eye/metrics` - 应用指标
- `/houyi-eye/prometheus` - Prometheus 格式指标

### API 文档

访问 `http://localhost:8080/swagger-ui.html` 查看 API 文档。

## 🧪 测试

```bash
# 运行所有测试
mvn test

# 运行特定测试
mvn test -Dtest=DecryptTest
```

## 📦 部署

### Docker 部署

```bash
# 构建镜像
docker build -t houyi:latest .

# 运行容器
docker run -d \
  --name houyi \
  --env-file .env \
  -p 8080:8080 \
  houyi:latest
```

### Systemd 服务

参考 [SECURITY.md](SECURITY.md) 中的 systemd 配置示例。

## 🔒 安全

**重要提示**: 

- ✅ 所有敏感信息都使用环境变量
- ✅ `.env` 文件已添加到 `.gitignore`
- ❌ 永远不要提交包含真实密钥的文件到 Git
- ❌ 不要在代码中硬编码密钥

详细的安全配置指南请参考 [SECURITY.md](SECURITY.md)。

## 📚 文档

> 💡 **提示**: 查看 [📖 文档索引](DOCS_INDEX.md) 获取完整的文档列表和导航

### 核心文档
- [系统架构文档](ARCHITECTURE.md) - 完整的系统架构和设计 🏗️
- [RocketMQ 5.0 配置指南](ROCKETMQ_5.0_GUIDE.md) - 消息队列配置详解 ⭐
- [企业微信配置指南](WEWORK_CONFIG_GUIDE.md) - 企业微信配置详解
- [安全配置指南](SECURITY.md) - 如何安全地配置敏感信息

### 技术文档
- [升级总结](UPGRADE_SUMMARY.md) - 技术栈升级详情
- [最终优化报告](FINAL_REPORT.md) - 代码质量优化报告
- [改进计划](IMPROVEMENT_PLAN.md) - 代码改进计划

## 🤝 贡献

欢迎提交 Issue 和 Pull Request！

## 📄 许可证

[MIT License](LICENSE)

## 📞 联系方式

如有问题，请提交 Issue 或联系维护者。

---

**代码质量**: A 级 ⭐⭐⭐⭐⭐  
**最后更新**: 2025-12-29

