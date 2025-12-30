# 部署说明 - 2025-12-30

## 本次更新内容

### 1. 修复日志警告
- 排除了 `commons-logging` 依赖冲突（aliyun-sdk-oss、ons-client、c3p0）
- 关闭了 logback 的 debug 模式

### 2. 企业微信配置
已配置单个企业微信：
- **Corp ID**: `ww129e1e186af00f38`
- **Secret**: `Ib9OlzyiU-NjcTZxOScXY9JXo315t1NYmuItZVhQgFE`
- **私钥文件**: `/etc/houyi/corp-1.pem`

### 3. 文件位置
- 本地 JAR 包: `/Users/rocky/Sites/dayu/target/houyi-0.0.1-SNAPSHOT.jar`
- 服务器临时位置: `/tmp/houyi-new.jar`

## 服务器部署步骤

### 1. 确保私钥文件存在
```bash
ssh root@106.14.70.42

# 检查私钥文件是否存在
ls -l /etc/houyi/corp-1.pem

# 如果不存在，需要创建
# mkdir -p /etc/houyi
# 然后将私钥内容写入 /etc/houyi/corp-1.pem
# chmod 600 /etc/houyi/corp-1.pem
```

### 2. 备份并部署新版本
```bash
ssh root@106.14.70.42

cd /home/admin/houyi

# 备份当前版本
cp houyi-0.0.1-SNAPSHOT.jar houyi-0.0.1-SNAPSHOT.jar.backup-$(date +%Y%m%d-%H%M%S)

# 部署新版本
mv /tmp/houyi-new.jar houyi-0.0.1-SNAPSHOT.jar

# 验证文件
ls -lh houyi-0.0.1-SNAPSHOT.jar
```

### 3. 修复服务器配置（已完成）
服务器上的 `/home/admin/houyi/config/application.yml` 已修复：
- 将 `org.hibernate.dialect.MySQL5InnoDBDialect` 改为 `org.hibernate.dialect.MySQLDialect`

### 4. 重启应用

#### 方式一：使用 Supervisor（推荐）
```bash
ssh root@106.14.70.42
supervisorctl restart houyi
supervisorctl status houyi
```

#### 方式二：手动启动
```bash
ssh root@106.14.70.42
cd /home/admin/houyi

# 停止旧进程（如果有）
pkill -f houyi-0.0.1-SNAPSHOT.jar

# 启动新进程
nohup java -Xms2g -Xmx2g -XX:+UseG1GC -XX:MaxGCPauseMillis=200 \
  -Dspring.profiles.active=prod \
  -Dspring.config.location=classpath:/application.yml,file:/home/admin/houyi/config/application.yml \
  -Dlog.path=/var/log/houyi \
  -jar houyi-0.0.1-SNAPSHOT.jar \
  >> /var/log/houyi/houyi.log 2>&1 &

# 查看日志
tail -f /var/log/houyi/houyi.log
```

### 5. 验证部署
```bash
# 检查进程
ps aux | grep houyi

# 检查日志
tail -f /var/log/houyi/houyi.log

# 检查健康状态
curl http://localhost:8080/houyi-eye/health

# 检查应用信息
curl http://localhost:8080/houyi-eye/info
```

## 配置文件说明

### wework-corps.yml（内置在 JAR 中）
```yaml
wework:
  corps:
    - corp-id: ww129e1e186af00f38
      corp-name: 主企业
      secret: Ib9OlzyiU-NjcTZxOScXY9JXo315t1NYmuItZVhQgFE
      private-key-file: /etc/houyi/corp-1.pem
      enabled: true
```

### 重要提醒
1. **私钥文件必须存在**: 确保 `/etc/houyi/corp-1.pem` 文件存在且可读
2. **文件权限**: 建议设置私钥文件权限为 600（仅所有者可读写）
3. **日志监控**: 启动后密切关注日志，确保没有错误

## 已知问题

### Supervisor 启动问题
如果使用 supervisor 启动遇到 "spawn error"，可能的原因：
1. 环境变量加载脚本 `/etc/houyi/load-env.sh` 有问题
2. 配置文件路径不正确
3. 私钥文件不存在或权限不对

建议先手动启动测试，确认无误后再使用 supervisor。

## 回滚方案
如果新版本有问题，可以快速回滚：
```bash
ssh root@106.14.70.42
cd /home/admin/houyi

# 停止当前版本
supervisorctl stop houyi
# 或
pkill -f houyi-0.0.1-SNAPSHOT.jar

# 恢复备份
cp houyi-0.0.1-SNAPSHOT.jar.backup-YYYYMMDD-HHMMSS houyi-0.0.1-SNAPSHOT.jar

# 重启
supervisorctl start houyi
```

## 联系信息
- 部署时间: 2025-12-30 19:57
- JAR 包大小: 152MB
- 服务器: 106.14.70.42

