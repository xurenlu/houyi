# 安全配置指南

## 🔒 敏感信息保护

本项目已经将所有敏感信息（密钥、密码等）改为从**环境变量**读取，确保不会泄露到 Git 仓库中。

---

## 📋 配置步骤

### 1. 复制环境变量模板

```bash
cp .env.example .env
```

### 2. 编辑 `.env` 文件，填入真实的值

```bash
# 使用你喜欢的编辑器
vim .env
# 或
nano .env
```

### 3. 确保 `.env` 文件不会被提交

`.env` 文件已经添加到 `.gitignore` 中，**永远不要**提交这个文件到 Git！

---

## 🔑 需要配置的敏感信息

### 必需配置（生产环境）

#### 1. 阿里云 OSS
```bash
ALIYUN_OSS_ACCESS_KEY=你的OSS_AccessKey
ALIYUN_OSS_ACCESS_SECRET=你的OSS_AccessSecret
```

#### 2. 阿里云 MNS
```bash
ALIYUN_MNS_ACCESS_KEY=你的MNS_AccessKey
ALIYUN_MNS_ACCESS_SECRET=你的MNS_AccessSecret
```

#### 3. 阿里云通用 AK/SK
```bash
ALIYUN_ACCESS_KEY=你的阿里云AccessKey
ALIYUN_ACCESS_SECRET=你的阿里云AccessSecret
```

#### 4. MySQL 数据库
```bash
MYSQL_HOST=你的MySQL主机地址
MYSQL_USERNAME=你的MySQL用户名
MYSQL_PASSWORD=你的MySQL密码
```

#### 5. Redis
```bash
REDIS_HOST=你的Redis主机地址
REDIS_PASSWORD=你的Redis密码
```

#### 6. RocketMQ
```bash
ROCKETMQ_NAME_SRV_ADDR=你的RocketMQ地址
ROCKETMQ_GROUP_ID=你的GroupID
ROCKETMQ_ENDPOINT=你的RocketMQ端点
```

---

## 🚀 不同环境的配置方式

### 开发环境

1. **使用 `.env` 文件**（推荐）

```bash
# 创建 .env 文件
cp .env.example .env
# 编辑并填入开发环境的配置
vim .env
```

2. **在 IDE 中配置环境变量**

- **IntelliJ IDEA**: Run → Edit Configurations → Environment Variables
- **VS Code**: 在 `launch.json` 中配置 `env` 字段

### 生产环境

#### 方式 1: 使用系统环境变量

```bash
# 在 ~/.bashrc 或 ~/.zshrc 中添加
export ALIYUN_OSS_ACCESS_KEY="你的密钥"
export ALIYUN_OSS_ACCESS_SECRET="你的密钥"
# ... 其他环境变量

# 重新加载配置
source ~/.bashrc
```

#### 方式 2: 使用 systemd 服务（推荐）

创建 `/etc/systemd/system/houyi.service`:

```ini
[Unit]
Description=Houyi WeChat Archive Service
After=network.target

[Service]
Type=simple
User=houyi
WorkingDirectory=/opt/houyi
ExecStart=/usr/bin/java -jar houyi.jar
Restart=always

# 环境变量配置
Environment="ALIYUN_OSS_ACCESS_KEY=你的密钥"
Environment="ALIYUN_OSS_ACCESS_SECRET=你的密钥"
Environment="MYSQL_HOST=你的数据库地址"
Environment="MYSQL_USERNAME=你的用户名"
Environment="MYSQL_PASSWORD=你的密码"
# ... 其他环境变量

[Install]
WantedBy=multi-user.target
```

#### 方式 3: 使用 Docker

```bash
# 方式 A: 使用 -e 参数
docker run -d \
  -e ALIYUN_OSS_ACCESS_KEY="你的密钥" \
  -e ALIYUN_OSS_ACCESS_SECRET="你的密钥" \
  -e MYSQL_HOST="你的数据库地址" \
  -e MYSQL_USERNAME="你的用户名" \
  -e MYSQL_PASSWORD="你的密码" \
  houyi:latest

# 方式 B: 使用 --env-file
docker run -d --env-file .env houyi:latest
```

#### 方式 4: 使用 Kubernetes ConfigMap/Secret

```yaml
apiVersion: v1
kind: Secret
metadata:
  name: houyi-secrets
type: Opaque
stringData:
  ALIYUN_OSS_ACCESS_KEY: "你的密钥"
  ALIYUN_OSS_ACCESS_SECRET: "你的密钥"
  MYSQL_PASSWORD: "你的密码"
---
apiVersion: v1
kind: Deployment
metadata:
  name: houyi
spec:
  template:
    spec:
      containers:
      - name: houyi
        image: houyi:latest
        envFrom:
        - secretRef:
            name: houyi-secrets
```

---

## ⚠️ 安全最佳实践

### 1. 永远不要提交敏感信息

- ✅ 使用环境变量
- ✅ 使用密钥管理服务（如阿里云 KMS）
- ❌ 不要硬编码在代码中
- ❌ 不要提交到 Git 仓库
- ❌ 不要写在配置文件中

### 2. 定期轮换密钥

- 建议每 90 天更换一次 AccessKey
- 使用阿里云 RAM 创建子账号，最小权限原则
- 为不同环境使用不同的密钥

### 3. 使用密钥管理服务

考虑使用：
- **阿里云 KMS** (Key Management Service)
- **HashiCorp Vault**
- **AWS Secrets Manager**

### 4. 监控和审计

- 启用阿里云 ActionTrail 审计日志
- 监控异常的 API 调用
- 设置告警规则

### 5. 网络安全

- 使用 VPC 内网访问
- 配置安全组规则
- 启用 HTTPS/TLS

---

## 🔍 检查是否泄露敏感信息

### 检查 Git 历史

```bash
# 搜索可能的密钥泄露
git log -p | grep -i "password\|secret\|key" | head -20

# 使用 git-secrets 工具
git secrets --scan
```

### 使用在线工具

- [GitGuardian](https://www.gitguardian.com/)
- [TruffleHog](https://github.com/trufflesecurity/trufflehog)

---

## 🆘 如果密钥已经泄露

### 立即行动：

1. **立即禁用泄露的密钥**
   - 登录阿里云控制台
   - 禁用或删除泄露的 AccessKey

2. **创建新的密钥**
   - 生成新的 AccessKey
   - 更新所有使用该密钥的服务

3. **清理 Git 历史**
   ```bash
   # 使用 BFG Repo-Cleaner 清理历史
   bfg --replace-text passwords.txt
   git reflog expire --expire=now --all
   git gc --prune=now --aggressive
   ```

4. **检查是否有异常访问**
   - 查看阿里云 ActionTrail 日志
   - 检查是否有异常的资源使用

5. **通知相关人员**
   - 通知团队成员
   - 如有必要，通知安全团队

---

## 📚 相关文档

- [阿里云 AccessKey 安全最佳实践](https://help.aliyun.com/document_detail/116401.html)
- [Spring Boot 外部化配置](https://docs.spring.io/spring-boot/docs/current/reference/html/features.html#features.external-config)
- [12-Factor App: Config](https://12factor.net/config)

---

## ✅ 配置检查清单

部署前请确认：

- [ ] 所有敏感信息都使用环境变量
- [ ] `.env` 文件已添加到 `.gitignore`
- [ ] 生产环境使用独立的密钥
- [ ] 已启用阿里云 ActionTrail 审计
- [ ] 已配置安全组规则
- [ ] 已设置密钥过期提醒
- [ ] 团队成员了解安全规范

---

**记住：安全无小事，保护好你的密钥！** 🔐

