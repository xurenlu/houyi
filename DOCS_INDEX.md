# 📚 后羿（Houyi）文档索引

欢迎来到后羿系统文档中心！本文档索引帮助您快速找到所需的文档。

---

## 🚀 快速开始

如果您是第一次使用后羿系统，建议按以下顺序阅读：

1. **[README.md](README.md)** - 项目概览和快速开始指南
2. **[SECURITY.md](SECURITY.md)** - 配置环境变量和敏感信息
3. **[WEWORK_CONFIG_GUIDE.md](WEWORK_CONFIG_GUIDE.md)** - 配置企业微信
4. **[ROCKETMQ_5.0_GUIDE.md](ROCKETMQ_5.0_GUIDE.md)** - 配置 RocketMQ 5.0
5. **[ARCHITECTURE.md](ARCHITECTURE.md)** - 理解系统架构

---

## 📖 文档分类

### 🏗️ 架构与设计

#### [ARCHITECTURE.md](ARCHITECTURE.md)
**系统架构文档** - 20KB

完整的系统架构设计文档，包括：
- 🎯 系统概述和核心功能
- 🏗️ 系统架构图（ASCII 图）
- 🔄 数据流转详解（正常流程 + 重试流程）
- 🗄️ 数据库设计（表结构 + 索引）
- 🧵 线程池配置
- 🔌 外部依赖清单
- 📦 核心组件说明
- 🚀 部署架构（单机 + 高可用）
- 📊 性能指标
- 🐛 常见问题排查

**适合人群**: 架构师、开发人员、运维人员

---

### ⚙️ 配置指南

#### [SECURITY.md](SECURITY.md)
**安全配置指南** - 5.9KB

如何安全地配置敏感信息：
- 🔐 环境变量配置方法
- 📝 `.env` 文件使用
- 🚀 生产环境部署配置
- 🐳 Docker 环境变量配置
- 🔧 Systemd 服务配置
- ⚠️ 安全注意事项

**适合人群**: 运维人员、开发人员

---

#### [WEWORK_CONFIG_GUIDE.md](WEWORK_CONFIG_GUIDE.md)
**企业微信配置指南** - 8.1KB

如何配置企业微信会话存档：
- 📋 配置方式（YAML vs 环境变量）
- 🔧 配置文件详解
- 🔑 获取企业微信凭证
- 🌐 IP 白名单配置
- 🔄 动态刷新配置
- 🧪 测试验证
- 🐛 常见问题

**适合人群**: 配置管理员、开发人员

---

#### [ROCKETMQ_5.0_GUIDE.md](ROCKETMQ_5.0_GUIDE.md)
**RocketMQ 5.0 配置指南** - 14KB ⭐

RocketMQ 5.0 消息队列详细配置：
- 📋 实例配置（1 个 RocketMQ 5.0 实例）
- 📊 Topic 配置（2 个 Topic：主队列 + 重试队列）
- 🏷️ Tag 配置（4 个 Tag）
- 👥 Consumer Group 配置
- 🔄 消息流转架构（统一 TCP 协议）
- ⚙️ 配置文件详解
- 🔧 环境变量配置
- 📝 代码组件说明
- 🔐 安全建议
- 📊 消息格式
- 🎯 在阿里云控制台创建资源
- 🧪 测试和监控
- 🔍 故障排查

**适合人群**: 运维人员、开发人员、架构师

---

### 📊 技术文档

#### [UPGRADE_SUMMARY.md](UPGRADE_SUMMARY.md)
**技术栈升级总结** - 6.0KB

记录了从 Java 8 + Spring Boot 2.5.2 升级到 Java 21 + Spring Boot 3.2.11 的过程：
- 🎯 升级目标和动机
- 📦 依赖版本变更清单
- 🔧 代码适配（javax → jakarta）
- ⚠️ 遇到的问题和解决方案
- ✅ 验证和测试
- 📈 性能对比

**适合人群**: 开发人员、技术负责人

---

#### [FINAL_REPORT.md](FINAL_REPORT.md)
**最终优化报告** - 8.4KB

代码质量优化的最终报告：
- 📊 优化成果总结
- 🎯 代码质量指标
- 🔧 主要改进点
- 📈 性能提升
- 🔒 安全性增强
- 📚 文档完善

**适合人群**: 项目经理、技术负责人、开发人员

---

#### [IMPROVEMENT_PLAN.md](IMPROVEMENT_PLAN.md)
**代码改进计划** - 3.1KB

未来的改进方向和计划：
- 🎯 P0 级别改进（必须完成）
- 🔧 P1 级别改进（重要）
- 📈 P2 级别改进（优化）
- 🔮 长期规划

**适合人群**: 技术负责人、开发人员

---

### 📝 历史记录

#### [REFACTORING_SUMMARY.md](REFACTORING_SUMMARY.md)
**重构总结** - 4.8KB

第一轮代码重构的总结：
- 🎯 重构目标
- 🔧 主要改进
- 📊 重构成果
- ⚠️ 注意事项

**适合人群**: 开发人员

---

#### [FINAL_OPTIMIZATION_REPORT.md](FINAL_OPTIMIZATION_REPORT.md)
**最终优化报告（详细版）** - 9.9KB

更详细的优化报告：
- 📊 优化前后对比
- 🔧 具体改进措施
- 📈 量化指标
- 🎯 优化效果

**适合人群**: 开发人员、技术负责人

---

#### [OPTIMIZATION_SUMMARY.md](OPTIMIZATION_SUMMARY.md)
**优化总结** - 6.6KB

代码优化的阶段性总结：
- 🎯 优化目标
- 🔧 优化内容
- 📊 优化效果
- 📝 经验总结

**适合人群**: 开发人员

---

#### [UPGRADE_PLAN.md](UPGRADE_PLAN.md)
**升级计划** - 3.1KB

技术栈升级的计划文档：
- 🎯 升级目标
- 📦 升级清单
- ⚠️ 风险评估
- 📅 升级步骤

**适合人群**: 技术负责人、开发人员

---

## 🎯 按角色查找文档

### 👨‍💼 项目经理 / 产品经理

建议阅读：
1. [README.md](README.md) - 了解项目概况
2. [ARCHITECTURE.md](ARCHITECTURE.md) - 理解系统架构
3. [FINAL_REPORT.md](FINAL_REPORT.md) - 查看项目质量

---

### 👨‍💻 开发人员

建议阅读：
1. [README.md](README.md) - 快速开始
2. [ARCHITECTURE.md](ARCHITECTURE.md) - 理解架构
3. [SECURITY.md](SECURITY.md) - 配置环境
4. [WEWORK_CONFIG_GUIDE.md](WEWORK_CONFIG_GUIDE.md) - 企业微信配置
5. [ROCKETMQ_5.0_GUIDE.md](ROCKETMQ_5.0_GUIDE.md) - RocketMQ 5.0 配置
6. [UPGRADE_SUMMARY.md](UPGRADE_SUMMARY.md) - 技术栈升级
7. [IMPROVEMENT_PLAN.md](IMPROVEMENT_PLAN.md) - 改进计划

---

### 🔧 运维人员

建议阅读：
1. [README.md](README.md) - 部署指南
2. [SECURITY.md](SECURITY.md) - 安全配置 ⭐
3. [WEWORK_CONFIG_GUIDE.md](WEWORK_CONFIG_GUIDE.md) - 企业微信配置
4. [ROCKETMQ_5.0_GUIDE.md](ROCKETMQ_5.0_GUIDE.md) - RocketMQ 5.0 配置 ⭐
5. [ARCHITECTURE.md](ARCHITECTURE.md) - 部署架构和故障排查

---

### 🏗️ 架构师

建议阅读：
1. [ARCHITECTURE.md](ARCHITECTURE.md) - 系统架构 ⭐
2. [ROCKETMQ_5.0_GUIDE.md](ROCKETMQ_5.0_GUIDE.md) - 消息队列设计
3. [UPGRADE_SUMMARY.md](UPGRADE_SUMMARY.md) - 技术选型
4. [FINAL_REPORT.md](FINAL_REPORT.md) - 代码质量
5. [IMPROVEMENT_PLAN.md](IMPROVEMENT_PLAN.md) - 未来规划

---

## 🔍 按主题查找文档

### 🚀 部署相关
- [README.md](README.md) - 快速开始和部署
- [SECURITY.md](SECURITY.md) - 安全配置
- [ARCHITECTURE.md](ARCHITECTURE.md) - 部署架构

### ⚙️ 配置相关
- [SECURITY.md](SECURITY.md) - 环境变量配置
- [WEWORK_CONFIG_GUIDE.md](WEWORK_CONFIG_GUIDE.md) - 企业微信配置
- [ROCKETMQ_5.0_GUIDE.md](ROCKETMQ_5.0_GUIDE.md) - RocketMQ 5.0 配置

### 🏗️ 架构相关
- [ARCHITECTURE.md](ARCHITECTURE.md) - 系统架构
- [ROCKETMQ_5.0_GUIDE.md](ROCKETMQ_5.0_GUIDE.md) - 消息队列架构

### 📊 代码质量
- [FINAL_REPORT.md](FINAL_REPORT.md) - 代码质量报告
- [REFACTORING_SUMMARY.md](REFACTORING_SUMMARY.md) - 重构总结
- [IMPROVEMENT_PLAN.md](IMPROVEMENT_PLAN.md) - 改进计划

### 🔧 技术升级
- [UPGRADE_SUMMARY.md](UPGRADE_SUMMARY.md) - 升级总结
- [UPGRADE_PLAN.md](UPGRADE_PLAN.md) - 升级计划

### 🐛 故障排查
- [ARCHITECTURE.md](ARCHITECTURE.md) - 常见问题
- [ROCKETMQ_5.0_GUIDE.md](ROCKETMQ_5.0_GUIDE.md) - RocketMQ 故障排查
- [WEWORK_CONFIG_GUIDE.md](WEWORK_CONFIG_GUIDE.md) - 企业微信问题

### 🔒 安全相关
- [SECURITY.md](SECURITY.md) - 安全配置

---

## 📊 文档统计

| 分类 | 文档数量 | 总大小 |
|------|---------|--------|
| 核心文档 | 4 | 48.1 KB |
| 配置指南 | 3 | 28.0 KB |
| 技术文档 | 3 | 17.5 KB |
| 历史记录 | 4 | 24.5 KB |
| **总计** | **14** | **118.1 KB** |

---

## 🔗 外部资源

### 官方文档
- [Spring Boot 3.2.11 文档](https://docs.spring.io/spring-boot/docs/3.2.11/reference/html/)
- [企业微信会话存档 API](https://developer.work.weixin.qq.com/document/path/91774)
- [阿里云 RocketMQ 文档](https://help.aliyun.com/product/29530.html)
- [阿里云 OSS 文档](https://help.aliyun.com/product/31815.html)

### 技术栈
- [Java 21 文档](https://docs.oracle.com/en/java/javase/21/)
- [Spring Data JPA 文档](https://docs.spring.io/spring-data/jpa/docs/current/reference/html/)
- [Jedis 文档](https://github.com/redis/jedis)
- [Lombok 文档](https://projectlombok.org/features/)

---

## 📝 文档维护

### 文档更新记录

| 日期 | 文档 | 更新内容 |
|------|------|---------|
| 2025-12-29 | ROCKETMQ_5.0_GUIDE.md | RocketMQ 5.0 配置指南 |
| 2025-12-29 | ARCHITECTURE.md | 新增系统架构文档 |
| 2025-12-29 | DOCS_INDEX.md | 新增文档索引 |
| 2025-12-29 | README.md | 更新文档链接 |

### 如何贡献文档

如果您发现文档有误或需要补充，请：

1. 提交 Issue 说明问题
2. 或者直接提交 Pull Request
3. 文档使用 Markdown 格式
4. 遵循现有的文档风格

---

## ❓ 常见问题

### Q: 我应该从哪个文档开始？
**A**: 建议从 [README.md](README.md) 开始，然后根据您的角色查看对应的文档。

### Q: 如何快速部署系统？
**A**: 按顺序阅读：
1. [README.md](README.md) - 快速开始
2. [SECURITY.md](SECURITY.md) - 配置环境变量
3. [WEWORK_CONFIG_GUIDE.md](WEWORK_CONFIG_GUIDE.md) - 配置企业微信
4. [ROCKETMQ_5.0_GUIDE.md](ROCKETMQ_5.0_GUIDE.md) - 配置 RocketMQ 5.0

### Q: 如何理解系统架构？
**A**: 直接阅读 [ARCHITECTURE.md](ARCHITECTURE.md)，里面有详细的架构图和说明。

### Q: 遇到问题如何排查？
**A**: 查看各文档的"故障排查"章节：
- [ARCHITECTURE.md](ARCHITECTURE.md) - 常见问题
- [ROCKETMQ_5.0_GUIDE.md](ROCKETMQ_5.0_GUIDE.md) - RocketMQ 问题
- [WEWORK_CONFIG_GUIDE.md](WEWORK_CONFIG_GUIDE.md) - 企业微信问题

### Q: 如何升级技术栈？
**A**: 参考 [UPGRADE_SUMMARY.md](UPGRADE_SUMMARY.md)，里面记录了完整的升级过程。

---

## 📞 获取帮助

如果您在使用过程中遇到问题：

1. 📖 **查阅文档** - 先查看本索引找到相关文档
2. 🔍 **搜索 Issue** - 在 GitHub Issues 中搜索类似问题
3. 💬 **提交 Issue** - 如果找不到答案，提交新的 Issue
4. 📧 **联系维护者** - 紧急问题可以直接联系维护者

---

**最后更新**: 2025-12-29  
**维护者**: Houyi Team

---

**返回**: [README.md](README.md) | **首页**: [DOCS_INDEX.md](DOCS_INDEX.md)

