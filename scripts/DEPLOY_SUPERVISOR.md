# Supervisor 部署指南

## 在服务器 106.14.70.42 上部署 Houyi 项目

### 前置要求
- 服务器 IP: 106.14.70.42
- 需要 root 权限
- 已准备好编译好的 jar 包: `houyi-0.0.1-SNAPSHOT.jar`

### 部署步骤

#### 1. 上传文件到服务器

```bash
# 在本地执行，上传安装脚本和配置文件
scp scripts/install-supervisor.sh root@106.14.70.42:/tmp/
scp scripts/supervisor-houyi.ini root@106.14.70.42:/tmp/
scp env.prod root@106.14.70.42:/tmp/
```

#### 2. 登录服务器

```bash
ssh root@106.14.70.42
```

#### 3. 执行安装脚本

```bash
chmod +x /tmp/install-supervisor.sh
/tmp/install-supervisor.sh
```

安装脚本会自动：
- 检查并安装 Java 21（如果未安装）
- 安装 Supervisor
- 创建必要的目录（/etc/houyi, /var/log/houyi, /opt/houyi）
- 配置 Supervisor 启动 Houyi 项目

#### 4. 配置环境变量

将 `env.prod` 文件内容复制到 `/etc/houyi/.env`：

```bash
cp /tmp/env.prod /etc/houyi/.env
chmod 600 /etc/houyi/.env  # 设置权限，保护敏感信息
```

#### 5. 上传 jar 包

```bash
# 在本地执行
scp target/houyi-0.0.1-SNAPSHOT.jar root@106.14.70.42:/opt/houyi/
```

或者在服务器上直接下载/构建。

#### 6. 启动服务

```bash
# 重新加载 Supervisor 配置
supervisorctl reread
supervisorctl update

# 启动 Houyi 服务
supervisorctl start houyi

# 查看状态
supervisorctl status houyi
```

### 常用命令

```bash
# 启动服务
supervisorctl start houyi

# 停止服务
supervisorctl stop houyi

# 重启服务
supervisorctl restart houyi

# 查看状态
supervisorctl status houyi

# 查看日志
tail -f /var/log/houyi/houyi_stdout.log
tail -f /var/log/houyi/houyi_stderr.log

# 查看应用日志（Spring Boot）
tail -f /var/log/houyi/houyi.log
```

### 验证部署

1. 检查服务状态：
   ```bash
   supervisorctl status houyi
   ```
   应该显示 `RUNNING`

2. 检查日志：
   ```bash
   tail -f /var/log/houyi/houyi_stdout.log
   ```
   应该看到 Spring Boot 启动日志

3. 检查端口（如果配置了端口）：
   ```bash
   netstat -tlnp | grep 8080
   ```

### 故障排查

#### 问题 1: Java 版本不对
```bash
# 检查 Java 版本
java -version

# 如果版本不对，手动安装 Java 21
# CentOS/RHEL:
yum install -y java-21-openjdk java-21-openjdk-devel

# Debian/Ubuntu:
apt-get update
apt-get install -y openjdk-21-jdk
```

#### 问题 2: 环境变量未加载
```bash
# 检查环境变量文件
cat /etc/houyi/.env

# 手动测试加载
source /etc/houyi/load-env.sh
echo $REDIS_HOST
```

#### 问题 3: jar 包不存在
```bash
# 检查 jar 包
ls -lh /opt/houyi/houyi-0.0.1-SNAPSHOT.jar
```

#### 问题 4: 查看详细错误
```bash
# 查看 Supervisor 日志
tail -f /var/log/supervisor/supervisord.log

# 查看应用错误日志
tail -f /var/log/houyi/houyi_stderr.log
```

### 更新部署

当需要更新 jar 包时：

```bash
# 1. 停止服务
supervisorctl stop houyi

# 2. 备份旧 jar 包
mv /opt/houyi/houyi-0.0.1-SNAPSHOT.jar /opt/houyi/houyi-0.0.1-SNAPSHOT.jar.bak

# 3. 上传新 jar 包
# (在本地执行)
scp target/houyi-0.0.1-SNAPSHOT.jar root@106.14.70.42:/opt/houyi/

# 4. 启动服务
supervisorctl start houyi
```

### 注意事项

1. **环境变量文件位置**: `/etc/houyi/.env` - 确保文件存在且格式正确
2. **配置文件**: 使用 `application-prod.yml`（通过 `-Dspring.profiles.active=prod` 指定）
3. **日志目录**: `/var/log/houyi/` - 确保有写入权限
4. **JVM 参数**: 默认 `-Xms2g -Xmx2g`，可根据服务器配置调整
5. **自动重启**: Supervisor 配置了自动重启，服务异常退出会自动重启

