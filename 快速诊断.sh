#!/bin/bash

# 快速诊断脚本 - 直接检查常见问题

echo "=========================================="
echo "HandNote 快速诊断工具"
echo "=========================================="
echo ""

# 检查 ADB
if ! command -v adb &> /dev/null; then
    echo "❌ ADB 未安装"
    echo ""
    echo "📱 请使用以下方法查看日志："
    echo ""
    echo "方法1：应用内查看（最简单）"
    echo "  1. 打开应用"
    echo "  2. 点击底部'设置'"
    echo "  3. 点击'查看日志'卡片"
    echo ""
    echo "方法2：检查 Downloads 文件夹"
    echo "  1. 打开文件管理器"
    echo "  2. 进入 Downloads 文件夹"
    echo "  3. 查找 HandNote_crash_*.txt 文件"
    echo ""
    exit 0
fi

# 检查设备
if ! adb devices | grep -q "device$"; then
    echo "❌ 未检测到设备"
    echo "请连接设备并启用 USB 调试"
    exit 1
fi

echo "✅ 设备已连接"
echo ""

# 检查应用
if ! adb shell pm list packages | grep -q "com.handnote.app"; then
    echo "❌ 应用未安装"
    exit 1
fi

echo "✅ 应用已安装"
echo ""

# 1. 检查崩溃报告
echo "=========================================="
echo "1. 崩溃报告"
echo "=========================================="
CRASH=$(adb shell run-as com.handnote.app cat files/crash_report.txt 2>/dev/null)
if [ -z "$CRASH" ]; then
    echo "✅ 没有崩溃报告"
else
    echo "❌ 发现崩溃报告："
    echo "$CRASH" | head -20
fi
echo ""

# 2. 检查最新日志
echo "=========================================="
echo "2. 最新日志（最后50行）"
echo "=========================================="
TODAY=$(date +%Y-%m-%d)
LOG=$(adb shell run-as com.handnote.app cat "files/logs/app_${TODAY}.log" 2>/dev/null)
if [ -z "$LOG" ]; then
    echo "📝 今天的日志文件不存在"
    LATEST=$(adb shell run-as com.handnote.app ls -t files/logs/ 2>/dev/null | head -1)
    if [ -n "$LATEST" ]; then
        echo "读取最新日志文件: $LATEST"
        LOG=$(adb shell run-as com.handnote.app cat "files/logs/$LATEST" 2>/dev/null)
    fi
fi

if [ -n "$LOG" ]; then
    echo "$LOG" | tail -50
    echo ""
    echo "--- 错误统计 ---"
    ERRORS=$(echo "$LOG" | grep -c "ERROR" || echo "0")
    WARNS=$(echo "$LOG" | grep -c "WARN" || echo "0")
    echo "错误: $ERRORS 条"
    echo "警告: $WARNS 条"
    
    if [ "$ERRORS" -gt 0 ]; then
        echo ""
        echo "最近的错误："
        echo "$LOG" | grep "ERROR" | tail -5
    fi
else
    echo "📝 没有找到日志文件"
fi
echo ""

# 3. 检查 Downloads 文件夹
echo "=========================================="
echo "3. Downloads 文件夹中的崩溃日志"
echo "=========================================="
DOWNLOADS=$(adb shell ls /sdcard/Download/HandNote_crash_*.txt 2>/dev/null | head -3)
if [ -z "$DOWNLOADS" ]; then
    echo "✅ Downloads 文件夹中没有崩溃日志"
else
    echo "发现崩溃日志文件："
    echo "$DOWNLOADS"
    echo ""
    LATEST=$(adb shell ls -t /sdcard/Download/HandNote_crash_*.txt 2>/dev/null | head -1)
    if [ -n "$LATEST" ]; then
        echo "最新崩溃日志内容："
        adb shell cat "$LATEST" | head -30
    fi
fi
echo ""

# 4. 检查 Logcat（实时错误）
echo "=========================================="
echo "4. Logcat 实时错误（最后20条）"
echo "=========================================="
adb logcat -d -s MainActivity:* FileLogger:* AndroidRuntime:E *:E | tail -20
echo ""

echo "=========================================="
echo "诊断完成"
echo "=========================================="

