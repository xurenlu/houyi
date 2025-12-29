# 企业微信配置指南

## 📋 概述

本系统从**本地配置文件**读取企业微信信息。这样做的好处：

- ✅ **减少外部依赖**：无需依赖外部 API 服务
- ✅ **提高稳定性**：避免因外部服务故障导致系统无法启动
- ✅ **提高安全性**：敏感信息完全由自己掌控
- ✅ **简化部署**：配置更加直观，易于管理
- ✅ **支持多企业**：可以轻松配置多个企业微信

---

## 🚀 快速开始

### 方式 1: 使用环境变量（推荐）

1. **复制环境变量模板**
```bash
cp .env.example .env
```

2. **编辑 `.env` 文件，配置企业信息**
```bash
# 企业 1
WEWORK_CORP1_ID=ww1234567890abcdef
WEWORK_CORP1_NAME=我的企业
WEWORK_CORP1_SECRET=your_secret_here
WEWORK_CORP1_PRIVATE_KEY=-----BEGIN PRIVATE KEY-----\nMIIEvQIBADANBg...\n-----END PRIVATE KEY-----
WEWORK_CORP1_ENABLED=true

# 企业 2（如果有多个企业）
WEWORK_CORP2_ID=ww0987654321fedcba
WEWORK_CORP2_NAME=另一个企业
WEWORK_CORP2_SECRET=another_secret
WEWORK_CORP2_PRIVATE_KEY=-----BEGIN PRIVATE KEY-----\n...\n-----END PRIVATE KEY-----
WEWORK_CORP2_ENABLED=true
```

3. **启动应用**
```bash
mvn spring-boot:run
```

### 方式 2: 使用私钥文件（推荐）

1. **将私钥文件放在 `src/main/resources/keys/` 目录**
```bash
# 创建目录
mkdir -p src/main/resources/keys

# 复制私钥文件
cp /path/to/your/private-key.pem src/main/resources/keys/corp1-private-key.pem

# 设置文件权限
chmod 600 src/main/resources/keys/*.pem
```

2. **编辑 `src/main/resources/wework-corps.yml`**
```yaml
wework:
  corps:
    - corp-id: ww1234567890abcdef
      corp-name: 我的企业
      secret: your_secret_here
      private-key-file: classpath:keys/corp1-private-key.pem  # 从文件读取
      enabled: true

    - corp-id: ww0987654321fedcba
      corp-name: 另一个企业
      secret: another_secret
      private-key-file: /etc/houyi/keys/corp2-private-key.pem  # 绝对路径
      enabled: true
```

3. **启动应用**
```bash
mvn spring-boot:run
```

### 方式 3: 直接配置私钥内容

如果私钥较短或用于测试，也可以直接在配置文件中写入：

```yaml
wework:
  corps:
    - corp-id: ww1234567890abcdef
      corp-name: 我的企业
      secret: your_secret_here
      private-key: |
        -----BEGIN PRIVATE KEY-----
        MIIEvQIBADANBgkqhkiG9w0BAQEFAASCBKcwggSjAgEAAoIBAQC...
        -----END PRIVATE KEY-----
      enabled: true
```

---

## 📝 配置说明

### 配置项详解

| 字段 | 必填 | 说明 | 示例 |
|------|------|------|------|
| `corp-id` | ✅ | 企业微信 ID | `ww1234567890abcdef` |
| `corp-name` | ✅ | 企业名称（用于日志显示） | `我的企业` |
| `secret` | ✅ | 会话存档 Secret | `your_secret_here` |
| `private-key` | ⚠️ | 会话存档私钥内容 | `-----BEGIN PRIVATE KEY-----\n...` |
| `private-key-file` | ⚠️ | 私钥文件路径 | `classpath:keys/xxx.pem` 或 `/path/to/key.pem` |
| `enabled` | ❌ | 是否启用（默认 true） | `true` / `false` |

**注意**: `private-key` 和 `private-key-file` 二选一：
- 如果同时配置，优先使用 `private-key`
- 推荐使用 `private-key-file`，更易于管理

### 私钥配置方式

#### 方式 1: 使用私钥文件（推荐 ✅）

**优点**: 
- 易于管理和更新
- 不会让配置文件过长
- 更安全（文件权限控制）

**配置示例**:
```yaml
# 从 classpath 读取（开发环境）
private-key-file: classpath:keys/corp1-private-key.pem

# 从文件系统读取（生产环境推荐）
private-key-file: /etc/houyi/keys/corp1-private-key.pem

# 使用环境变量
private-key-file: ${WEWORK_CORP1_PRIVATE_KEY_FILE}
```

#### 方式 2: 直接配置私钥内容

**单行格式（使用 `\n` 换行）**:
```yaml
private-key: "-----BEGIN PRIVATE KEY-----\nMIIEvQIBADANBg...\n-----END PRIVATE KEY-----"
```

**多行格式（YAML 多行字符串）**:
```yaml
private-key: |
  -----BEGIN PRIVATE KEY-----
  MIIEvQIBADANBgkqhkiG9w0BAQEFAASCBKcwggSjAgEAAoIBAQC...
  -----END PRIVATE KEY-----
```

---

## 🔄 配置更新

### 添加新企业

#### 方式 1: 修改配置文件

编辑 `wework-corps.yml`，添加新的企业配置：

```yaml
wework:
  corps:
    # ... 现有企业 ...
    
    # 新企业
    - corp-id: ww_new_corp_id
      corp-name: 新企业
      secret: new_secret
      private-key: |
        -----BEGIN PRIVATE KEY-----
        ...
        -----END PRIVATE KEY-----
      enabled: true
```

重启应用后生效。

#### 方式 2: 使用环境变量

添加新的环境变量：

```bash
# 在 .env 文件中添加
WEWORK_CORP3_ID=ww_new_corp_id
WEWORK_CORP3_NAME=新企业
WEWORK_CORP3_SECRET=new_secret
WEWORK_CORP3_PRIVATE_KEY=your_private_key
WEWORK_CORP3_ENABLED=true
```

在 `wework-corps.yml` 中引用：

```yaml
wework:
  corps:
    # ... 现有企业 ...
    
    - corp-id: ${WEWORK_CORP3_ID:}
      corp-name: ${WEWORK_CORP3_NAME:}
      secret: ${WEWORK_CORP3_SECRET:}
      private-key: ${WEWORK_CORP3_PRIVATE_KEY:}
      enabled: ${WEWORK_CORP3_ENABLED:true}
```

### 禁用企业

将 `enabled` 设置为 `false`：

```yaml
- corp-id: ww1234567890abcdef
  corp-name: 我的企业
  secret: your_secret
  private-key: your_key
  enabled: false  # 禁用此企业
```

或者使用环境变量：

```bash
WEWORK_CORP1_ENABLED=false
```

### 热更新配置

目前配置在应用启动时加载，修改配置后需要**重启应用**才能生效。

如果需要热更新，可以调用刷新接口（需要先实现）：

```bash
curl -X POST http://localhost:8080/api/admin/refresh-corp-config
```

---

## 🔍 验证配置

### 1. 查看启动日志

应用启动时会输出配置信息：

```
开始同步企业配置到数据库...
创建新企业配置: ww1234567890abcdef
企业配置已保存: ww1234567890abcdef - 我的企业
企业配置同步完成，成功同步 2 个企业
发现 2 个启用的企业配置
准备启动企业: 我的企业 (ww1234567890abcdef)
准备启动企业: 另一个企业 (ww0987654321fedcba)
=== 所有企业消息处理线程已启动 ===
```

### 2. 检查数据库

配置会自动同步到 `corp_info` 表：

```sql
SELECT corpid, corpname, status FROM corp_info;
```

### 3. 查看健康检查

访问健康检查端点：

```bash
curl http://localhost:8080/houyi-eye/health
```

---

## 🔧 高级配置

### 使用 Kubernetes ConfigMap

```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: wework-config
data:
  wework-corps.yml: |
    wework:
      corps:
        - corp-id: ww1234567890abcdef
          corp-name: 我的企业
          secret: your_secret
          private-key: |
            -----BEGIN PRIVATE KEY-----
            ...
            -----END PRIVATE KEY-----
          enabled: true
```

挂载到 Pod：

```yaml
spec:
  containers:
  - name: houyi
    volumeMounts:
    - name: config
      mountPath: /app/config/wework-corps.yml
      subPath: wework-corps.yml
  volumes:
  - name: config
    configMap:
      name: wework-config
```

### 使用 Kubernetes Secret（推荐）

```yaml
apiVersion: v1
kind: Secret
metadata:
  name: wework-secrets
type: Opaque
stringData:
  WEWORK_CORP1_SECRET: your_secret
  WEWORK_CORP1_PRIVATE_KEY: |
    -----BEGIN PRIVATE KEY-----
    ...
    -----END PRIVATE KEY-----
```

---

## ⚠️ 注意事项

### 1. 私钥格式

- 私钥必须是 **PKCS#8** 格式
- 必须包含 `-----BEGIN PRIVATE KEY-----` 和 `-----END PRIVATE KEY-----`
- 如果是 PKCS#1 格式（`BEGIN RSA PRIVATE KEY`），需要转换

转换命令：
```bash
openssl pkcs8 -topk8 -inform PEM -outform PEM -nocrypt \
  -in rsa_private_key.pem -out private_key.pem
```

### 2. 配置文件安全

- ✅ 使用环境变量存储敏感信息
- ✅ 不要将包含真实密钥的配置文件提交到 Git
- ✅ 生产环境使用 Kubernetes Secret 或其他密钥管理服务
- ❌ 不要在配置文件中硬编码密钥

### 3. 配置优先级

配置加载优先级（从高到低）：

1. 环境变量
2. `wework-corps.yml` 文件
3. 默认值

---

## 🆚 与旧版本的区别

### 旧版本（使用外部 API）

```java
// 从外部 API 获取配置
YapiResult result = corpInfoApi.getCorpInfo(corpId);
corpInfo.setSecret(result.getData().getMessageSecret());
corpInfo.setPrikey(result.getData().getMessageKey());
```

**问题**：
- ❌ 依赖外部 API
- ❌ 网络故障会导致启动失败
- ❌ 配置分散在外部系统

### 新版本（本地配置）

```yaml
# 配置文件
wework:
  corps:
    - corp-id: ww123
      secret: your_secret
      private-key: your_key
```

**优势**：
- ✅ 无外部依赖
- ✅ 配置集中管理
- ✅ 启动更快更稳定
- ✅ 支持多种配置方式

---

## 📚 相关文档

- [SECURITY.md](SECURITY.md) - 安全配置指南
- [README.md](README.md) - 项目文档
- [.env.example](.env.example) - 环境变量模板

---

## 🆘 常见问题

### Q: 如何获取企业微信的配置信息？

A: 登录企业微信管理后台：
1. 进入「管理工具」→「会话内容存档」
2. 查看「Secret」和「私钥」
3. 企业 ID 在「我的企业」→「企业信息」中查看

### Q: 配置修改后不生效？

A: 需要重启应用。配置在启动时加载，运行时修改不会自动生效。

### Q: 可以同时配置多少个企业？

A: 理论上没有限制，但建议根据服务器性能合理配置。每个企业会启动一个独立的消息处理线程。

### Q: 如何临时禁用某个企业？

A: 将该企业的 `enabled` 设置为 `false`，然后重启应用。

---

**最后更新**: 2025-12-29

