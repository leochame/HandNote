#!/bin/bash

# 查看日志并分析错误的脚本
# 使用方法：./查看日志并分析错误.sh

echo "=========================================="
echo "HandNote 日志分析工具"
echo "=========================================="
echo ""

# 检查 ADB 是否可用
if ! command -v adb &> /dev/null; then
    echo "❌ 错误: 未找到 ADB 工具"
    echo ""
    echo "请确保："
    echo "1. 已安装 Android SDK Platform Tools"
    echo "2. ADB 已添加到 PATH 环境变量"
    echo ""
    echo "或者："
    echo "1. 在应用内查看日志："
    echo "   - 打开应用"
    echo "   - 点击底部导航栏的'设置'"
    echo "   - 点击'查看日志'卡片"
    echo "   - 查看'应用日志'和'崩溃报告'标签页"
    echo ""
    echo "2. 检查 Downloads 文件夹："
    echo "   - 打开文件管理器"
    echo "   - 进入 Downloads 文件夹"
    echo "   - 查找 HandNote_crash_*.txt 文件"
    echo ""
    exit 1
fi

# 检查设备连接
if ! adb devices | grep -q "device$"; then
    echo "❌ 错误: 未检测到已连接的 Android 设备"
    echo ""
    echo "请确保："
    echo "1. 设备已通过 USB 连接"
    echo "2. 已启用 USB 调试"
    echo "3. 已授权此计算机进行 USB 调试"
    echo ""
    exit 1
fi

echo "✅ 设备已连接"
echo ""

# 检查应用是否已安装
if ! adb shell pm list packages | grep -q "com.handnote.app"; then
    echo "❌ 错误: 未找到 HandNote 应用"
    echo ""
    echo "请确保应用已安装"
    exit 1
fi

echo "✅ 应用已安装"
echo ""

# 查看崩溃报告
echo "=========================================="
echo "1. 崩溃报告"
echo "=========================================="
CRASH_REPORT=$(adb shell run-as com.handnote.app cat files/crash_report.txt 2>/dev/null)
if [ -z "$CRASH_REPORT" ]; then
    echo "📝 没有崩溃报告（这是好事！）"
else
    echo "$CRASH_REPORT"
    echo ""
    echo "--- 错误分析 ---"
    if echo "$CRASH_REPORT" | grep -q "IllegalStateException"; then
        echo "⚠️  检测到 IllegalStateException（状态异常）"
        echo "   可能原因："
        echo "   - 数据库未正确初始化"
        echo "   - Repository 初始化失败"
        echo "   - ViewModel 初始化失败"
    fi
    if echo "$CRASH_REPORT" | grep -q "NullPointerException"; then
        echo "⚠️  检测到 NullPointerException（空指针异常）"
        echo "   可能原因："
        echo "   - 对象未初始化"
        echo "   - 空值未检查"
    fi
    if echo "$CRASH_REPORT" | grep -q "SQLiteException"; then
        echo "⚠️  检测到 SQLiteException（数据库异常）"
        echo "   可能原因："
        echo "   - 数据库文件损坏"
        echo "   - 数据库版本不兼容"
        echo "   - SQL 语句错误"
    fi
    if echo "$CRASH_REPORT" | grep -q "FileNotFoundException"; then
        echo "⚠️  检测到 FileNotFoundException（文件未找到）"
        echo "   可能原因："
        echo "   - 日志文件路径错误"
        echo "   - 存储权限未授予"
    fi
fi
echo ""

# 查看最新的日志文件
echo "=========================================="
echo "2. 最新的应用日志"
echo "=========================================="
TODAY=$(date +%Y-%m-%d)
LOG_FILE="files/logs/app_${TODAY}.log"
LOG_CONTENT=$(adb shell run-as com.handnote.app cat "$LOG_FILE" 2>/dev/null)

if [ -z "$LOG_CONTENT" ]; then
    echo "📝 今天的日志文件不存在，尝试查找其他日志文件..."
    LOG_FILES=$(adb shell run-as com.handnote.app ls files/logs/ 2>/dev/null)
    if [ -z "$LOG_FILES" ]; then
        echo "📝 没有找到任何日志文件"
    else
        echo "找到的日志文件："
        echo "$LOG_FILES"
        echo ""
        echo "尝试读取最新的日志文件..."
        LATEST_LOG=$(adb shell run-as com.handnote.app ls -t files/logs/ 2>/dev/null | head -1)
        if [ -n "$LATEST_LOG" ]; then
            LOG_CONTENT=$(adb shell run-as com.handnote.app cat "files/logs/$LATEST_LOG" 2>/dev/null)
            echo "$LOG_CONTENT"
        fi
    fi
else
    echo "$LOG_CONTENT"
fi
echo ""

# 分析日志中的错误
if [ -n "$LOG_CONTENT" ]; then
    echo "--- 日志错误分析 ---"
    ERROR_COUNT=$(echo "$LOG_CONTENT" | grep -c "ERROR" || echo "0")
    WARN_COUNT=$(echo "$LOG_CONTENT" | grep -c "WARN" || echo "0")
    
    echo "错误数量: $ERROR_COUNT"
    echo "警告数量: $WARN_COUNT"
    echo ""
    
    if [ "$ERROR_COUNT" -gt 0 ]; then
        echo "最近的错误："
        echo "$LOG_CONTENT" | grep "ERROR" | tail -5
        echo ""
    fi
    
    if [ "$WARN_COUNT" -gt 0 ]; then
        echo "最近的警告："
        echo "$LOG_CONTENT" | grep "WARN" | tail -5
        echo ""
    fi
fi

# 列出所有日志文件
echo "=========================================="
echo "3. 所有日志文件列表"
echo "=========================================="
adb shell run-as com.handnote.app ls -lah files/logs/ 2>/dev/null || echo "无法列出日志文件"
echo ""

# 检查 Downloads 文件夹中的崩溃日志
echo "=========================================="
echo "4. Downloads 文件夹中的崩溃日志"
echo "=========================================="
DOWNLOADS_CRASH=$(adb shell ls /sdcard/Download/HandNote_crash_*.txt 2>/dev/null | head -5)
if [ -z "$DOWNLOADS_CRASH" ]; then
    echo "📝 Downloads 文件夹中没有崩溃日志文件"
else
    echo "找到的崩溃日志文件："
    echo "$DOWNLOADS_CRASH"
    echo ""
    echo "最新的崩溃日志内容："
    LATEST_CRASH=$(adb shell ls -t /sdcard/Download/HandNote_crash_*.txt 2>/dev/null | head -1)
    if [ -n "$LATEST_CRASH" ]; then
        adb shell cat "$LATEST_CRASH"
    fi
fi
echo ""

echo "=========================================="
echo "分析完成"
echo "=========================================="
echo ""
echo "提示："
echo "- 如果应用崩溃，请查看上面的崩溃报告"
echo "- 查看应用日志可以了解崩溃前的操作"
echo "- 崩溃日志也会自动保存到 Downloads 文件夹"
echo "- 在应用内可以通过'设置' -> '查看日志'查看日志"

