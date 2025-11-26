#!/bin/bash
# locust-tv Termux 定时任务配置脚本
# 用途: 在 Termux 中设置每日自动更新 IPTV 频道列表

set -e

echo "=========================================="
echo "locust-tv Termux 定时任务配置"
echo "=========================================="

# 检查是否在 Termux 环境
if [ ! -d "$PREFIX" ]; then
    echo "错误: 此脚本仅支持 Termux 环境"
    exit 1
fi

# 安装依赖
echo "[1/4] 安装依赖包..."
pkg install -y python cronie termux-services

# 获取脚本绝对路径
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PYTHON_SCRIPT="$SCRIPT_DIR/update_iptv.py"

if [ ! -f "$PYTHON_SCRIPT" ]; then
    echo "错误: 找不到 update_iptv.py 脚本"
    exit 1
fi

# 确保脚本可执行
chmod +x "$PYTHON_SCRIPT"

# 创建日志目录
LOG_DIR="$HOME/.locust-tv/logs"
mkdir -p "$LOG_DIR"

echo "[2/4] 配置 cron 任务..."

# 创建 cron 任务 (每天凌晨 3 点执行)
CRON_JOB="0 3 * * * python3 $PYTHON_SCRIPT >> $LOG_DIR/update.log 2>&1"

# 检查 cron 任务是否已存在
if crontab -l 2>/dev/null | grep -q "update_iptv.py"; then
    echo "检测到已存在的 cron 任务, 将更新..."
    crontab -l 2>/dev/null | grep -v "update_iptv.py" | crontab -
fi

# 添加新的 cron 任务
(crontab -l 2>/dev/null; echo "$CRON_JOB") | crontab -

echo "[3/4] 启动 crond 服务..."
sv-enable crond
sv up crond

echo "[4/4] 执行首次更新..."
python3 "$PYTHON_SCRIPT"

echo "=========================================="
echo "配置完成!"
echo "=========================================="
echo "定时任务: 每天凌晨 3:00 自动更新"
echo "日志路径: $LOG_DIR/update.log"
echo "频道文件: /data/local/tmp/locust-tv/channels.m3u"
echo ""
echo "查看 cron 任务: crontab -l"
echo "查看服务状态: sv status crond"
echo "手动执行更新: python3 $PYTHON_SCRIPT"
echo "=========================================="
