#!/bin/bash
# Locust TV APK 构建脚本
# 用途: 在本地或 CI 环境中构建 Debug APK

set -e

echo "=========================================="
echo "Locust TV APK 构建脚本"
echo "=========================================="

# 检查环境
if [ ! -f "gradlew" ]; then
    echo "错误: 找不到 gradlew，请在项目根目录执行此脚本"
    exit 1
fi

# 清理旧的构建产物
echo "[1/4] 清理旧的构建产物..."
./gradlew clean

# 构建 Debug APK
echo "[2/4] 构建 Debug APK..."
./gradlew assembleDebug --stacktrace

# 检查构建产物
APK_PATH="app/build/outputs/apk/debug/app-debug.apk"
if [ ! -f "$APK_PATH" ]; then
    echo "错误: APK 构建失败，找不到 $APK_PATH"
    exit 1
fi

# 创建 release 目录
echo "[3/4] 准备发布文件..."
mkdir -p release
cp "$APK_PATH" release/locust-tv-alpha.apk

# 显示 APK 信息
echo "[4/4] 构建完成!"
echo "=========================================="
echo "APK 路径: release/locust-tv-alpha.apk"
echo "APK 大小: $(du -h release/locust-tv-alpha.apk | cut -f1)"
echo "=========================================="

# 提示上传到 GitHub Release
echo ""
echo "上传到 GitHub Release:"
echo "gh release upload v0.1.0-alpha release/locust-tv-alpha.apk --repo yanzhao77/locust-tv"
