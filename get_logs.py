#!/usr/bin/env python3
"""
日志获取工具 - 让 Agent 可以直接获取应用日志
通过 ADB 从设备获取日志文件并保存到工作区，方便 Agent 直接读取
"""

import os
import subprocess
import sys
from datetime import datetime
from pathlib import Path

# 应用包名
APP_PACKAGE = "com.handnote.app"

# 工作区日志目录
LOGS_DIR = Path(__file__).parent / "logs"
LOGS_DIR.mkdir(exist_ok=True)


def check_adb():
    """检查 ADB 是否可用"""
    try:
        result = subprocess.run(
            ["adb", "version"],
            capture_output=True,
            text=True,
            timeout=5
        )
        return result.returncode == 0
    except (FileNotFoundError, subprocess.TimeoutExpired):
        return False


def check_device():
    """检查设备是否连接"""
    try:
        result = subprocess.run(
            ["adb", "devices"],
            capture_output=True,
            text=True,
            timeout=5
        )
        return "device" in result.stdout
    except (FileNotFoundError, subprocess.TimeoutExpired):
        return False


def check_app_installed():
    """检查应用是否已安装"""
    try:
        result = subprocess.run(
            ["adb", "shell", "pm", "list", "packages", APP_PACKAGE],
            capture_output=True,
            text=True,
            timeout=5
        )
        return APP_PACKAGE in result.stdout
    except (FileNotFoundError, subprocess.TimeoutExpired):
        return False


def get_crash_report():
    """获取崩溃报告"""
    try:
        result = subprocess.run(
            ["adb", "shell", "run-as", APP_PACKAGE, "cat", "files/crash_report.txt"],
            capture_output=True,
            text=True,
            timeout=10
        )
        if result.returncode == 0 and result.stdout.strip():
            return result.stdout
        return None
    except Exception as e:
        print(f"获取崩溃报告失败: {e}", file=sys.stderr)
        return None


def get_log_files():
    """获取所有日志文件列表"""
    try:
        result = subprocess.run(
            ["adb", "shell", "run-as", APP_PACKAGE, "ls", "files/logs/"],
            capture_output=True,
            text=True,
            timeout=10
        )
        if result.returncode == 0:
            files = [f.strip() for f in result.stdout.strip().split("\n") if f.strip()]
            # 过滤出日志文件
            log_files = [f for f in files if f.startswith("app_") and f.endswith(".log")]
            return log_files
        return []
    except Exception as e:
        print(f"获取日志文件列表失败: {e}", file=sys.stderr)
        return []


def get_latest_log_content():
    """获取最新日志文件的内容"""
    try:
        # 先尝试获取今天的日志
        today = datetime.now().strftime("%Y-%m-%d")
        log_file = f"files/logs/app_{today}.log"
        
        result = subprocess.run(
            ["adb", "shell", "run-as", APP_PACKAGE, "cat", log_file],
            capture_output=True,
            text=True,
            timeout=10
        )
        
        if result.returncode == 0 and result.stdout.strip():
            return result.stdout
        
        # 如果今天的日志不存在，获取最新的日志文件
        log_files = get_log_files()
        if log_files:
            # 获取最新的日志文件（按时间排序）
            result = subprocess.run(
                ["adb", "shell", "run-as", APP_PACKAGE, "ls", "-t", "files/logs/"],
                capture_output=True,
                text=True,
                timeout=10
            )
            if result.returncode == 0:
                files = [f.strip() for f in result.stdout.strip().split("\n") if f.strip()]
                log_files = [f for f in files if f.startswith("app_") and f.endswith(".log")]
                if log_files:
                    latest = log_files[0]
                    result = subprocess.run(
                        ["adb", "shell", "run-as", APP_PACKAGE, "cat", f"files/logs/{latest}"],
                        capture_output=True,
                        text=True,
                        timeout=10
                    )
                    if result.returncode == 0:
                        return result.stdout
        
        return None
    except Exception as e:
        print(f"获取日志内容失败: {e}", file=sys.stderr)
        return None


def save_logs_to_workspace():
    """将日志保存到工作区"""
    timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
    
    # 保存崩溃报告
    crash_report = get_crash_report()
    if crash_report:
        crash_file = LOGS_DIR / f"crash_report_{timestamp}.txt"
        crash_file.write_text(crash_report, encoding="utf-8")
        print(f"✅ 崩溃报告已保存: {crash_file}")
    else:
        print("📝 没有崩溃报告")
    
    # 保存应用日志
    log_content = get_latest_log_content()
    if log_content:
        log_file = LOGS_DIR / f"app_log_{timestamp}.txt"
        log_file.write_text(log_content, encoding="utf-8")
        print(f"✅ 应用日志已保存: {log_file}")
        
        # 统计错误和警告
        error_count = log_content.count("[ERROR]")
        warn_count = log_content.count("[WARN]")
        print(f"📊 日志统计: {error_count} 个错误, {warn_count} 个警告")
        
        return str(log_file)
    else:
        print("📝 没有找到日志文件")
        return None


def get_latest_logs():
    """获取最新日志内容（返回字符串，供 Agent 直接使用）"""
    crash_report = get_crash_report()
    log_content = get_latest_log_content()
    
    result = []
    
    if crash_report:
        result.append("=" * 50)
        result.append("崩溃报告")
        result.append("=" * 50)
        result.append(crash_report)
        result.append("")
    
    if log_content:
        result.append("=" * 50)
        result.append("应用日志")
        result.append("=" * 50)
        result.append(log_content)
        result.append("")
        
        # 统计信息
        error_count = log_content.count("[ERROR]")
        warn_count = log_content.count("[WARN]")
        result.append("=" * 50)
        result.append(f"统计: {error_count} 个错误, {warn_count} 个警告")
        result.append("=" * 50)
    
    if not result:
        return "没有找到日志文件"
    
    return "\n".join(result)


def main():
    """主函数"""
    if len(sys.argv) > 1 and sys.argv[1] == "--save":
        # 保存模式：将日志保存到工作区
        print("=" * 50)
        print("HandNote 日志获取工具 - 保存模式")
        print("=" * 50)
        print()
        
        if not check_adb():
            print("❌ 错误: 未找到 ADB 工具")
            print("请确保已安装 Android SDK Platform Tools 并添加到 PATH")
            sys.exit(1)
        
        if not check_device():
            print("❌ 错误: 未检测到已连接的 Android 设备")
            print("请确保设备已通过 USB 连接并启用 USB 调试")
            sys.exit(1)
        
        if not check_app_installed():
            print(f"❌ 错误: 未找到 {APP_PACKAGE} 应用")
            sys.exit(1)
        
        print("✅ 设备已连接")
        print("✅ 应用已安装")
        print()
        
        save_logs_to_workspace()
        
    else:
        # 默认模式：直接输出日志内容
        if not check_adb() or not check_device() or not check_app_installed():
            print("无法获取日志（ADB 不可用或设备未连接）")
            print("使用 --save 参数可以将日志保存到工作区")
            sys.exit(1)
        
        print(get_latest_logs())


if __name__ == "__main__":
    main()

