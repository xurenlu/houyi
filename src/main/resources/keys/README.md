# 企业微信私钥文件目录

## 📋 说明

此目录用于存放企业微信会话存档的私钥文件。

## 📁 文件命名建议

建议按以下格式命名私钥文件：

```
{corpId}-private-key.pem
```

例如：
- `ww1234567890abcdef-private-key.pem`
- `corp1-private-key.pem`
- `company-a-private-key.pem`

## 🔐 安全注意事项

1. **不要提交到 Git**
   - 私钥文件已在 `.gitignore` 中排除
   - 确保不会意外提交敏感文件

2. **文件权限**
   ```bash
   # 设置私钥文件权限（仅所有者可读）
   chmod 600 src/main/resources/keys/*.pem
   ```

3. **生产环境**
   - 建议将私钥文件放在应用目录外
   - 使用绝对路径引用：`/etc/houyi/keys/xxx.pem`

## 📝 配置方式

### 方式 1: 使用 classpath（开发环境）

将私钥文件放在此目录，然后在 `wework-corps.yml` 中配置：

```yaml
wework:
  corps:
    - corp-id: ww1234567890abcdef
      corp-name: 我的企业
      secret: your_secret
      private-key-file: classpath:keys/ww1234567890abcdef-private-key.pem
      enabled: true
```

### 方式 2: 使用文件系统路径（生产环境推荐）

将私钥文件放在安全的系统目录，例如 `/etc/houyi/keys/`：

```yaml
wework:
  corps:
    - corp-id: ww1234567890abcdef
      corp-name: 我的企业
      secret: your_secret
      private-key-file: /etc/houyi/keys/ww1234567890abcdef-private-key.pem
      enabled: true
```

### 方式 3: 使用环境变量

```yaml
wework:
  corps:
    - corp-id: ww1234567890abcdef
      corp-name: 我的企业
      secret: your_secret
      private-key-file: ${WEWORK_CORP1_PRIVATE_KEY_FILE}
      enabled: true
```

然后设置环境变量：
```bash
export WEWORK_CORP1_PRIVATE_KEY_FILE=/etc/houyi/keys/corp1-private-key.pem
```

## 📄 私钥文件格式

私钥文件应该是 PEM 格式：

```
-----BEGIN PRIVATE KEY-----
MIIEvQIBADANBgkqhkiG9w0BAQEFAASCBKcwggSjAgEAAoIBAQC...
...
-----END PRIVATE KEY-----
```

## 🔄 与直接配置私钥的对比

| 配置方式 | 优点 | 缺点 | 适用场景 |
|---------|------|------|---------|
| `private-key` | 配置简单 | 私钥很长，不易管理 | 简单测试 |
| `private-key-file` | 易于管理，安全性高 | 需要管理文件 | 生产环境 ✅ |

## 💡 最佳实践

1. **开发环境**: 使用 `classpath:keys/xxx.pem`
2. **生产环境**: 使用 `/etc/houyi/keys/xxx.pem`
3. **容器环境**: 使用 Volume 挂载私钥文件
4. **安全要求高**: 使用密钥管理服务（如 Vault）

---

**注意**: 此目录下的 `*.pem` 和 `*.key` 文件会被 Git 忽略，确保私钥安全。

