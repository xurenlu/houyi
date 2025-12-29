#!/bin/bash

###############################################################################
# 后羿（Houyi）启动脚本
# 用于启动企业微信会话存档系统
###############################################################################

# 设置颜色输出
RED='\033[0:31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 脚本目录
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"

# 配置
APP_NAME="houyi"
JAR_NAME="${APP_NAME}-0.0.1-SNAPSHOT.jar"
JAR_PATH="${PROJECT_DIR}/target/${JAR_NAME}"
PID_FILE="${PROJECT_DIR}/${APP_NAME}.pid"
LOG_DIR="${LOG_PATH:-/var/log/houyi}"
LOG_FILE="${LOG_DIR}/${APP_NAME}.log"

# JVM 参数
JVM_OPTS="${JVM_OPTS:--Xms2g -Xmx4g -XX:+UseG1GC -XX:MaxGCPauseMillis=200}"

# Spring Profile
SPRING_PROFILE="${SPRING_PROFILES_ACTIVE:-prod}"

###############################################################################
# 函数定义
###############################################################################

# 打印信息
print_info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

# 打印警告
print_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

# 打印错误
print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# 检查是否正在运行
is_running() {
    if [ -f "$PID_FILE" ]; then
        PID=$(cat "$PID_FILE")
        if ps -p "$PID" > /dev/null 2>&1; then
            return 0
        else
            rm -f "$PID_FILE"
            return 1
        fi
    fi
    return 1
}

# 启动应用
start() {
    print_info "正在启动 ${APP_NAME}..."
    
    # 检查是否已经在运行
    if is_running; then
        print_warn "${APP_NAME} 已经在运行中 (PID: $(cat "$PID_FILE"))"
        return 1
    fi
    
    # 检查 JAR 文件是否存在
    if [ ! -f "$JAR_PATH" ]; then
        print_error "JAR 文件不存在: ${JAR_PATH}"
        print_info "请先运行: mvn clean package -DskipTests"
        return 1
    fi
    
    # 创建日志目录
    mkdir -p "$LOG_DIR"
    
    # 加载环境变量
    if [ -f "${PROJECT_DIR}/.env" ]; then
        print_info "加载环境变量: ${PROJECT_DIR}/.env"
        export $(grep -v '^#' "${PROJECT_DIR}/.env" | xargs)
    else
        print_warn "未找到 .env 文件，使用默认配置"
    fi
    
    # 启动应用
    print_info "JVM 参数: ${JVM_OPTS}"
    print_info "Spring Profile: ${SPRING_PROFILE}"
    print_info "日志文件: ${LOG_FILE}"
    
    nohup java ${JVM_OPTS} \
        -Dspring.profiles.active=${SPRING_PROFILE} \
        -Dlog.path=${LOG_DIR} \
        -jar "${JAR_PATH}" \
        >> "${LOG_FILE}" 2>&1 &
    
    PID=$!
    echo $PID > "$PID_FILE"
    
    # 等待启动
    sleep 3
    
    if is_running; then
        print_info "${APP_NAME} 启动成功 (PID: ${PID})"
        print_info "查看日志: tail -f ${LOG_FILE}"
        return 0
    else
        print_error "${APP_NAME} 启动失败"
        print_info "查看日志: tail -100 ${LOG_FILE}"
        return 1
    fi
}

# 停止应用
stop() {
    print_info "正在停止 ${APP_NAME}..."
    
    if ! is_running; then
        print_warn "${APP_NAME} 未运行"
        return 1
    fi
    
    PID=$(cat "$PID_FILE")
    print_info "发送 SIGTERM 信号到进程 ${PID}..."
    kill "$PID"
    
    # 等待进程结束
    for i in {1..30}; do
        if ! ps -p "$PID" > /dev/null 2>&1; then
            rm -f "$PID_FILE"
            print_info "${APP_NAME} 已停止"
            return 0
        fi
        sleep 1
    done
    
    # 强制结束
    print_warn "进程未响应，发送 SIGKILL 信号..."
    kill -9 "$PID"
    rm -f "$PID_FILE"
    print_info "${APP_NAME} 已强制停止"
    return 0
}

# 重启应用
restart() {
    print_info "正在重启 ${APP_NAME}..."
    stop
    sleep 2
    start
}

# 查看状态
status() {
    if is_running; then
        PID=$(cat "$PID_FILE")
        print_info "${APP_NAME} 正在运行 (PID: ${PID})"
        
        # 显示进程信息
        echo ""
        ps -p "$PID" -o pid,ppid,user,%cpu,%mem,vsz,rss,tty,stat,start,time,command
        
        return 0
    else
        print_info "${APP_NAME} 未运行"
        return 1
    fi
}

# 查看日志
logs() {
    if [ -f "$LOG_FILE" ]; then
        tail -f "$LOG_FILE"
    else
        print_error "日志文件不存在: ${LOG_FILE}"
        return 1
    fi
}

# 显示帮助
usage() {
    echo "用法: $0 {start|stop|restart|status|logs}"
    echo ""
    echo "命令:"
    echo "  start    - 启动应用"
    echo "  stop     - 停止应用"
    echo "  restart  - 重启应用"
    echo "  status   - 查看状态"
    echo "  logs     - 查看日志"
    echo ""
    echo "环境变量:"
    echo "  SPRING_PROFILES_ACTIVE  - Spring Profile (默认: prod)"
    echo "  JVM_OPTS                - JVM 参数 (默认: -Xms2g -Xmx4g)"
    echo "  LOG_PATH                - 日志目录 (默认: /var/log/houyi)"
    echo ""
    echo "示例:"
    echo "  $0 start"
    echo "  SPRING_PROFILES_ACTIVE=dev $0 start"
    echo "  JVM_OPTS='-Xms4g -Xmx8g' $0 start"
}

###############################################################################
# 主程序
###############################################################################

case "$1" in
    start)
        start
        ;;
    stop)
        stop
        ;;
    restart)
        restart
        ;;
    status)
        status
        ;;
    logs)
        logs
        ;;
    *)
        usage
        exit 1
        ;;
esac

exit $?

