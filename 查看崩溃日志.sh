#!/bin/bash

# 查看崩溃日志的脚本

echo "=== 查看崩溃报告 ==="
adb shell run-as com.handnote.app cat files/crash_report.txt

echo ""
echo "=== 查看最新的应用日志 ==="
TODAY=$(date +%Y-%m-%d)
adb shell run-as com.handnote.app cat "files/logs/app_${TODAY}.log"

echo ""
echo "=== 列出所有日志文件 ==="
adb shell run-as com.handnote.app ls -la files/logs/

