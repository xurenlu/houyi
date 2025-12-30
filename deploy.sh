#!/bin/bash

# Redis MQ 部署脚本
# 用于快速部署到服务器

set -e

echo "=========================================="
echo "开始部署 Redis MQ 应用"
echo "=========================================="

# 检查参数
if [ -z "$1" ]; then
    echo "用法: ./deploy.sh <服务器地址> [用户名]"
    echo "示例: ./deploy.sh 192.168.1.100 root"
    exit 1
fi

SERVER=$1
USER=${2:-root}
APP_DIR="/opt/houyi"
JAR_NAME="houyi-0.0.1-SNAPSHOT.jar"

echo "服务器: $USER@$SERVER"
echo "应用目录: $APP_DIR"

# 1. 编译打包
echo ""
echo "步骤 1: 编译打包..."
mvn clean package -DskipTests

if [ ! -f "target/$JAR_NAME" ]; then
    echo "错误: JAR 文件不存在: target/$JAR_NAME"
    exit 1
fi

echo "✅ 编译完成"

# 2. 上传文件
echo ""
echo "步骤 2: 上传文件到服务器..."
ssh $USER@$SERVER "mkdir -p $APP_DIR"
scp target/$JAR_NAME $USER@$SERVER:$APP_DIR/
scp sql/delay_message_table.sql $USER@$SERVER:$APP_DIR/

echo "✅ 文件上传完成"

# 3. 执行数据库脚本（可选）
echo ""
read -p "是否执行数据库脚本创建表？(y/n) " -n 1 -r
echo
if [[ $REPLY =~ ^[Yy]$ ]]; then
    echo "步骤 3: 执行数据库脚本..."
    echo "请手动执行以下命令："
    echo "  mysql -u用户名 -p数据库名 < $APP_DIR/delay_message_table.sql"
fi

# 4. 创建 systemd 服务文件
echo ""
echo "步骤 4: 创建 systemd 服务..."
cat > /tmp/houyi.service <<EOF
[Unit]
Description=Houyi Application
After=network.target mysql.service redis.service

[Service]
Type=simple
User=$USER
WorkingDirectory=$APP_DIR
Environment="JAVA_HOME=/usr/lib/jvm/java-21"
Environment="SPRING_PROFILES_ACTIVE=prod"
Environment="REDIS_MQ_ENABLED=true"
ExecStart=/usr/bin/java -jar $APP_DIR/$JAR_NAME
Restart=always
RestartSec=10
StandardOutput=journal
StandardError=journal

[Install]
WantedBy=multi-user.target
EOF

scp /tmp/houyi.service $USER@$SERVER:/tmp/
ssh $USER@$SERVER "sudo mv /tmp/houyi.service /etc/systemd/system/ && sudo systemctl daemon-reload"

echo "✅ 服务文件创建完成"

# 5. 启动服务
echo ""
read -p "是否现在启动服务？(y/n) " -n 1 -r
echo
if [[ $REPLY =~ ^[Yy]$ ]]; then
    echo "步骤 5: 启动服务..."
    ssh $USER@$SERVER "sudo systemctl enable houyi && sudo systemctl restart houyi"
    echo "✅ 服务已启动"
    echo ""
    echo "查看服务状态:"
    ssh $USER@$SERVER "sudo systemctl status houyi"
fi

echo ""
echo "=========================================="
echo "部署完成！"
echo "=========================================="
echo ""
echo "常用命令："
echo "  查看日志: sudo journalctl -u houyi -f"
echo "  重启服务: sudo systemctl restart houyi"
echo "  停止服务: sudo systemctl stop houyi"
echo "  查看状态: sudo systemctl status houyi"
echo ""

