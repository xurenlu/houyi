#!/bin/bash

###############################################################################
# 后羿（Houyi）Supervisor 安装脚本
# 用于在服务器上安装 supervisor 和配置项目启动
###############################################################################

set -e

# 颜色输出
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

print_info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

print_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# 检查是否为 root 用户
if [ "$EUID" -ne 0 ]; then 
    print_error "请使用 root 用户运行此脚本"
    exit 1
fi

print_info "开始安装 Supervisor 和配置 Houyi 项目..."

# 1. 安装 Java 21
print_info "检查 Java 版本..."
if command -v java &> /dev/null; then
    JAVA_VERSION=$(java -version 2>&1 | head -n 1 | cut -d'"' -f2 | sed '/^1\./s///' | cut -d'.' -f1)
    if [ "$JAVA_VERSION" -ge 21 ]; then
        print_info "Java $JAVA_VERSION 已安装"
    else
        print_warn "当前 Java 版本为 $JAVA_VERSION，需要 Java 21 或更高版本"
        print_info "开始安装 Java 21..."
        
        # 检测系统类型
        if [ -f /etc/redhat-release ]; then
            # CentOS/RHEL
            yum install -y java-21-openjdk java-21-openjdk-devel
        elif [ -f /etc/debian_version ]; then
            # Debian/Ubuntu
            apt-get update
            apt-get install -y openjdk-21-jdk
        else
            print_error "不支持的系统类型，请手动安装 Java 21"
            exit 1
        fi
    fi
else
    print_info "Java 未安装，开始安装 Java 21..."
    if [ -f /etc/redhat-release ]; then
        yum install -y java-21-openjdk java-21-openjdk-devel
    elif [ -f /etc/debian_version ]; then
        apt-get update
        apt-get install -y openjdk-21-jdk
    else
        print_error "不支持的系统类型，请手动安装 Java 21"
        exit 1
    fi
fi

# 设置 JAVA_HOME
JAVA_HOME=$(readlink -f $(which java) | sed "s:bin/java::")
export JAVA_HOME
print_info "JAVA_HOME: $JAVA_HOME"

# 2. 安装 Supervisor
print_info "安装 Supervisor..."
if command -v supervisorctl &> /dev/null; then
    print_info "Supervisor 已安装"
else
    if [ -f /etc/redhat-release ]; then
        yum install -y supervisor
    elif [ -f /etc/debian_version ]; then
        apt-get update
        apt-get install -y supervisor
    else
        print_error "不支持的系统类型，请手动安装 Supervisor"
        exit 1
    fi
fi

# 3. 创建必要的目录
print_info "创建必要的目录..."
mkdir -p /etc/houyi
mkdir -p /var/log/houyi
mkdir -p /home/admin/houyi

# 4. 创建 Supervisor 配置文件
print_info "创建 Supervisor 配置文件..."
cat > /etc/supervisord.d/houyi.ini << 'EOF'
[program:houyi]
command=/usr/bin/java -Xms2g -Xmx2g -XX:+UseG1GC -XX:MaxGCPauseMillis=200 -Dspring.profiles.active=prod -Dlog.path=/var/log/houyi -jar /home/admin/houyi/houyi-0.0.1-SNAPSHOT.jar
directory=/home/admin/houyi
user=root
autostart=true
autorestart=true
startsecs=10
startretries=3
stopwaitsecs=30
stdout_logfile=/var/log/houyi/houyi_stdout.log
stdout_logfile_maxbytes=50MB
stdout_logfile_backups=10
stderr_logfile=/var/log/houyi/houyi_stderr.log
stderr_logfile_maxbytes=50MB
stderr_logfile_backups=10
environment=JAVA_HOME="%(ENV_JAVA_HOME)s"
EOF

# 5. 创建环境变量加载脚本
print_info "创建环境变量加载脚本..."
cat > /etc/houyi/load-env.sh << 'EOF'
#!/bin/bash
# 从 /etc/houyi/.env 加载环境变量
if [ -f /etc/houyi/.env ]; then
    export $(grep -v '^#' /etc/houyi/.env | grep -v '^$' | xargs)
fi
EOF

chmod +x /etc/houyi/load-env.sh

# 6. 更新 Supervisor 配置以加载环境变量
print_info "更新 Supervisor 配置以加载环境变量..."
cat > /etc/supervisord.d/houyi.ini << 'EOF'
[program:houyi]
command=/bin/bash -c "source /etc/houyi/load-env.sh && /usr/bin/java -Xms2g -Xmx2g -XX:+UseG1GC -XX:MaxGCPauseMillis=200 -Dspring.profiles.active=prod -Dlog.path=/var/log/houyi -jar /home/admin/houyi/houyi-0.0.1-SNAPSHOT.jar"
directory=/home/admin/houyi
user=root
autostart=true
autorestart=true
startsecs=10
startretries=3
stopwaitsecs=30
stdout_logfile=/var/log/houyi/houyi_stdout.log
stdout_logfile_maxbytes=50MB
stdout_logfile_backups=10
stderr_logfile=/var/log/houyi/houyi_stderr.log
stderr_logfile_maxbytes=50MB
stderr_logfile_backups=10
EOF

# 7. 启动 Supervisor
print_info "启动 Supervisor 服务..."
systemctl enable supervisord
systemctl start supervisord

# 8. 检查 Supervisor 状态
if systemctl is-active --quiet supervisord; then
    print_info "Supervisor 服务已启动"
else
    print_error "Supervisor 服务启动失败"
    exit 1
fi

print_info "安装完成！"
print_info ""
print_info "下一步操作："
print_info "1. 将 env.prod 文件内容复制到 /etc/houyi/.env"
print_info "2. 确保 jar 包已存在于 /home/admin/houyi/houyi-0.0.1-SNAPSHOT.jar"
print_info "3. 使用以下命令管理服务："
print_info "   - 启动: supervisorctl start houyi"
print_info "   - 停止: supervisorctl stop houyi"
print_info "   - 重启: supervisorctl restart houyi"
print_info "   - 状态: supervisorctl status houyi"
print_info "   - 日志: tail -f /var/log/houyi/houyi_stdout.log"

