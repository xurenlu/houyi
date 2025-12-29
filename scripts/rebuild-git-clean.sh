#!/bin/bash

###############################################################################
# Git 仓库重建脚本（清理敏感信息）
# 适用于项目初期，直接重建干净的 Git 历史
###############################################################################

set -e

# 颜色输出
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
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

print_step() {
    echo -e "${BLUE}[STEP]${NC} $1"
}

# 脚本目录
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"

cd "$PROJECT_DIR"

echo "============================================"
echo "  Git 仓库重建工具（清理敏感信息）"
echo "============================================"
echo ""

# 警告
print_warn "⚠️  此操作将删除所有 Git 历史并重建！"
print_warn "⚠️  所有提交记录将丢失！"
print_warn "⚠️  建议先备份仓库！"
echo ""
read -p "是否继续？(yes/no): " confirm

if [ "$confirm" != "yes" ]; then
    print_info "已取消操作"
    exit 0
fi

# 检查是否有未提交的更改
if ! git diff-index --quiet HEAD --; then
    print_error "检测到未提交的更改，请先提交或暂存"
    git status
    exit 1
fi

# 备份远程仓库 URL（如果有）
print_step "1. 保存远程仓库信息..."
REMOTE_URL=$(git remote get-url origin 2>/dev/null || echo "")
if [ -n "$REMOTE_URL" ]; then
    print_info "远程仓库: $REMOTE_URL"
else
    print_info "未配置远程仓库"
fi
echo ""

# 删除 .git 目录
print_step "2. 删除旧的 Git 历史..."
rm -rf .git
print_info "旧历史已删除"
echo ""

# 重新初始化 Git
print_step "3. 初始化新的 Git 仓库..."
git init
git branch -M main
print_info "Git 仓库已初始化"
echo ""

# 确保 .gitignore 存在
print_step "4. 检查 .gitignore..."
if [ ! -f .gitignore ]; then
    print_warn ".gitignore 不存在，创建默认配置"
    cat > .gitignore << 'EOF'
# 环境变量
.env
.env.local
.env.*.local

# 配置文件
application-local.yml
*-local.yml

# 敏感信息
*.key
*.pem
secrets/
credentials/

# 编译输出
target/
build/
out/
*.class
*.jar
*.war

# IDE
.idea/
*.iml
.vscode/
*.swp
*.swo
*~

# 日志
logs/
*.log

# 系统文件
.DS_Store
Thumbs.db

# Maven
.mvn/
mvnw
mvnw.cmd

# 临时文件
*.tmp
*.bak
*.backup
EOF
    print_info ".gitignore 已创建"
else
    print_info ".gitignore 已存在"
fi
echo ""

# 验证当前配置文件没有敏感信息
print_step "5. 验证配置文件..."
SENSITIVE_FOUND=0

# 检查 YAML 文件
for file in src/main/resources/application*.yml; do
    if [ -f "$file" ]; then
        if grep -q "LTAI[0-9a-zA-Z]\{20,\}" "$file" 2>/dev/null; then
            print_error "❌ $file 中发现 AccessKey"
            SENSITIVE_FOUND=1
        fi
        if grep -E "password:.*[^$\{]" "$file" | grep -v "password: \${" | grep -q "password:"; then
            print_warn "⚠️  $file 中可能有硬编码密码"
        fi
    fi
done

if [ $SENSITIVE_FOUND -eq 1 ]; then
    print_error "发现敏感信息，请先清理配置文件！"
    exit 1
fi

print_info "✅ 配置文件检查通过"
echo ""

# 添加所有文件
print_step "6. 添加文件到 Git..."
git add .
print_info "文件已添加"
echo ""

# 创建初始提交
print_step "7. 创建初始提交..."
git commit -m "Initial commit: Houyi 企业微信会话存档系统

项目特点:
- 企业微信会话存档
- 支持多企业配置
- RocketMQ 5.0 消息队列
- 阿里云 OSS 存储
- Redis 缓存
- MySQL 数据持久化

安全措施:
- 所有敏感信息使用环境变量
- 提供 .env.example 模板
- 完善的安全配置文档

技术栈:
- Java 21
- Spring Boot 3.2.11
- RocketMQ 5.0
- MySQL 8.0
- Redis
- 阿里云 OSS"

print_info "初始提交已创建"
echo ""

# 恢复远程仓库（如果有）
if [ -n "$REMOTE_URL" ]; then
    print_step "8. 重新配置远程仓库..."
    git remote add origin "$REMOTE_URL"
    print_info "远程仓库已配置: $REMOTE_URL"
    echo ""
    
    print_warn "⚠️  需要强制推送到远程仓库："
    echo "    git push -f origin main"
    echo ""
fi

# 显示状态
print_step "9. 当前状态..."
git log --oneline
echo ""

# 完成
echo "============================================"
print_info "✅ Git 仓库重建完成！"
echo "============================================"
echo ""
print_info "后续步骤:"
echo "  1. 验证代码: git log"
echo "  2. 测试编译: mvn clean compile"
if [ -n "$REMOTE_URL" ]; then
echo "  3. 强制推送: git push -f origin main"
echo "  4. 通知协作者重新克隆仓库"
fi
echo ""
print_warn "重要提醒:"
echo "  - 所有旧的提交历史已删除"
echo "  - 如果已推送到远程，需要强制推送"
echo "  - 协作者需要删除本地仓库并重新克隆"
echo "  - 记得更换所有可能泄露的凭证"
echo ""

