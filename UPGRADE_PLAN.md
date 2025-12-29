# 依赖升级计划

## 升级日期
2025-12-29

## 当前状态
- **Java**: 1.8
- **Spring Boot**: 2.5.2 (2021年)
- **本机 Java 版本**: Java 21 (已安装)

## 升级目标
- **Java**: 21 (LTS)
- **Spring Boot**: 3.2.11 (最新稳定版)
- **所有依赖**: 升级到最新兼容版本

---

## 主要变更

### 1. Java 版本升级
- **从**: Java 1.8
- **到**: Java 21 (LTS)
- **影响**: 可以使用现代 Java 特性（records, pattern matching, virtual threads 等）

### 2. Spring Boot 升级
- **从**: 2.5.2
- **到**: 3.2.11
- **重大变更**:
  - 最低 Java 版本要求: Java 17+
  - Jakarta EE 9+: `javax.*` → `jakarta.*`
  - Spring Security 6.x
  - Hibernate 6.x

### 3. 依赖升级列表

#### 核心依赖
| 依赖 | 当前版本 | 新版本 | 说明 |
|------|---------|--------|------|
| Spring Boot | 2.5.2 | 3.2.11 | 主框架升级 |
| Java | 1.8 | 21 | 语言版本 |

#### 第三方依赖
| 依赖 | 当前版本 | 新版本 | 说明 |
|------|---------|--------|------|
| commons-io | 2.4 | 2.15.1 | 安全更新 |
| okhttp3 | 3.12.13 | 4.12.0 | 主版本升级 |
| jedis | 3.6.1 | 5.1.0 | 主版本升级 |
| logback-classic | 1.2.3 | 1.4.14 | 由 Spring Boot 管理 |
| h2 | 1.4.199 | 2.2.224 | 主版本升级 |
| commons-codec | 1.15 | 1.16.0 | 小版本更新 |
| json | 20201115 | 20231013 | 更新到最新 |
| c3p0 | 0.9.5.5 | 0.10.1 | 主版本升级 |
| micrometer-registry-prometheus | 1.7.2 | 由 Spring Boot 管理 | |
| aliyun-sdk-oss | 3.10.2 | 3.18.1 | 更新到最新 |
| mysql-connector-java | - | 8.3.0 | 更新到最新 |

#### 需要替换的依赖
| 旧依赖 | 新依赖 | 原因 |
|--------|--------|------|
| springfox-swagger2 | springdoc-openapi | Springfox 不支持 Spring Boot 3 |
| junit 4.13.2 | junit-jupiter | Spring Boot 3 默认使用 JUnit 5 |

---

## 迁移步骤

### 第 1 步: 更新 pom.xml
1. 升级 Java 版本到 21
2. 升级 Spring Boot 到 3.2.11
3. 更新所有依赖版本
4. 替换不兼容的依赖

### 第 2 步: 包名迁移 (javax → jakarta)
需要全局替换以下包名：
- `javax.annotation` → `jakarta.annotation`
- `javax.persistence` → `jakarta.persistence`
- `javax.validation` → `jakarta.validation`
- `javax.servlet` → `jakarta.servlet`

### 第 3 步: API 更新
1. 更新已废弃的 API 调用
2. 修复 Jedis 5.x API 变更
3. 更新 OkHttp 4.x API 变更

### 第 4 步: 测试
1. 编译项目
2. 运行单元测试
3. 运行集成测试
4. 手动测试核心功能

---

## 风险评估

### 高风险
- **Jakarta EE 迁移**: 需要全局替换包名
- **Jedis 5.x**: API 有重大变更
- **Spring Boot 3.x**: 配置和行为变更

### 中风险
- **OkHttp 4.x**: API 有一些变更
- **Swagger 替换**: 需要更新 API 文档配置

### 低风险
- **其他依赖**: 主要是版本号更新，API 兼容

---

## 预计工作量
- **pom.xml 更新**: 30 分钟
- **包名迁移**: 1 小时
- **API 更新**: 1-2 小时
- **测试和修复**: 2-3 小时

**总计**: 约 4-6 小时

---

## 回滚计划
如果升级失败，可以：
1. 使用 Git 回滚到升级前的提交
2. 恢复备份的 pom.xml
3. 重新构建项目

