#!/bin/bash

###############################################################################
# 后羿（Houyi）系统服务安装脚本
# 用于将应用安装为 systemd 服务
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

# 检查是否以 root 运行
if [ "$EUID" -ne 0 ]; then
    print_error "请使用 root 权限运行此脚本"
    echo "使用: sudo $0"
    exit 1
fi

# 脚本目录
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"

# 配置
APP_NAME="houyi"
INSTALL_DIR="/opt/${APP_NAME}"
LOG_DIR="/var/log/${APP_NAME}"
SERVICE_FILE="${SCRIPT_DIR}/${APP_NAME}.service"
SYSTEMD_DIR="/etc/systemd/system"

print_info "开始安装 ${APP_NAME} 服务..."

# 1. 创建用户和组
if ! id -u "${APP_NAME}" > /dev/null 2>&1; then
    print_info "创建用户: ${APP_NAME}"
    useradd -r -s /bin/false -d "${INSTALL_DIR}" "${APP_NAME}"
else
    print_info "用户已存在: ${APP_NAME}"
fi

# 2. 创建安装目录
print_info "创建安装目录: ${INSTALL_DIR}"
mkdir -p "${INSTALL_DIR}"

# 3. 复制 JAR 文件
if [ -f "${PROJECT_DIR}/target/${APP_NAME}-0.0.1-SNAPSHOT.jar" ]; then
    print_info "复制 JAR 文件..."
    cp "${PROJECT_DIR}/target/${APP_NAME}-0.0.1-SNAPSHOT.jar" "${INSTALL_DIR}/"
else
    print_error "JAR 文件不存在，请先编译项目"
    echo "运行: mvn clean package -DskipTests"
    exit 1
fi

# 4. 复制配置文件
if [ -f "${PROJECT_DIR}/.env.example" ]; then
    if [ ! -f "${INSTALL_DIR}/.env" ]; then
        print_info "复制环境变量模板..."
        cp "${PROJECT_DIR}/.env.example" "${INSTALL_DIR}/.env"
        print_warn "请编辑 ${INSTALL_DIR}/.env 填入真实配置"
    else
        print_info ".env 文件已存在，跳过复制"
    fi
fi

# 5. 复制 native 库
if [ -d "${PROJECT_DIR}/jniLibs" ]; then
    print_info "复制 native 库..."
    cp -r "${PROJECT_DIR}/jniLibs" "${INSTALL_DIR}/"
fi

# 6. 创建日志目录
print_info "创建日志目录: ${LOG_DIR}"
mkdir -p "${LOG_DIR}"

# 7. 设置权限
print_info "设置文件权限..."
chown -R "${APP_NAME}:${APP_NAME}" "${INSTALL_DIR}"
chown -R "${APP_NAME}:${APP_NAME}" "${LOG_DIR}"
chmod 640 "${INSTALL_DIR}/.env"

# 8. 安装 systemd 服务
print_info "安装 systemd 服务..."
cp "${SERVICE_FILE}" "${SYSTEMD_DIR}/${APP_NAME}.service"
systemctl daemon-reload

print_info "安装完成！"
echo ""
print_info "后续步骤:"
echo "  1. 编辑配置文件: vi ${INSTALL_DIR}/.env"
echo "  2. 启用服务: systemctl enable ${APP_NAME}"
echo "  3. 启动服务: systemctl start ${APP_NAME}"
echo "  4. 查看状态: systemctl status ${APP_NAME}"
echo "  5. 查看日志: journalctl -u ${APP_NAME} -f"
echo ""
print_info "常用命令:"
echo "  systemctl start ${APP_NAME}      # 启动服务"
echo "  systemctl stop ${APP_NAME}       # 停止服务"
echo "  systemctl restart ${APP_NAME}    # 重启服务"
echo "  systemctl status ${APP_NAME}     # 查看状态"
echo "  systemctl enable ${APP_NAME}     # 开机自启"
echo "  systemctl disable ${APP_NAME}    # 禁用自启"
echo "  journalctl -u ${APP_NAME} -f     # 查看日志"

