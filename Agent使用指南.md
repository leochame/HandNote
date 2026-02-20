# Agent 使用指南 - 日志获取

## 快速开始

Agent 可以直接运行以下命令获取应用日志：

```bash
python3 get_logs.py
```

这会直接输出最新的日志内容，包括崩溃报告和应用日志。

## 保存日志到工作区

如果需要保存日志文件以便后续分析：

```bash
python3 get_logs.py --save
```

日志文件会保存到 `logs/` 目录：
- `logs/crash_report_YYYYMMDD_HHMMSS.txt`
- `logs/app_log_YYYYMMDD_HHMMSS.txt`

保存后，Agent 可以直接读取这些文件：

```python
from pathlib import Path

# 读取最新的崩溃报告
crash_files = sorted(Path("logs").glob("crash_report_*.txt"))
if crash_files:
    latest_crash = crash_files[-1]
    crash_content = latest_crash.read_text(encoding="utf-8")
    print(crash_content)

# 读取最新的应用日志
log_files = sorted(Path("logs").glob("app_log_*.txt"))
if log_files:
    latest_log = log_files[-1]
    log_content = latest_log.read_text(encoding="utf-8")
    print(log_content)
```

## 在代码中直接调用

Agent 可以在代码中直接调用工具函数：

```python
import subprocess
import sys

# 获取日志内容（字符串）
result = subprocess.run(
    [sys.executable, "get_logs.py"],
    capture_output=True,
    text=True,
    timeout=30
)

if result.returncode == 0:
    logs = result.stdout
    # 分析日志
    if "[ERROR]" in logs:
        print("发现错误！")
        # 提取错误信息
        errors = [line for line in logs.split("\n") if "[ERROR]" in line]
        print(f"错误数量: {len(errors)}")
else:
    print(f"获取日志失败: {result.stderr}")
```

## 常见使用场景

### 场景1：检查应用是否有错误

```python
import subprocess
import sys

result = subprocess.run(
    [sys.executable, "get_logs.py"],
    capture_output=True,
    text=True,
    timeout=30
)

if result.returncode == 0:
    logs = result.stdout
    error_count = logs.count("[ERROR]")
    if error_count > 0:
        print(f"⚠️ 发现 {error_count} 个错误")
        # 提取最近的错误
        error_lines = [line for line in logs.split("\n") if "[ERROR]" in line]
        print("最近的错误：")
        for error in error_lines[-5:]:
            print(f"  - {error}")
    else:
        print("✅ 没有发现错误")
```

### 场景2：分析崩溃原因

```python
import subprocess
import sys

# 先保存日志到工作区
result = subprocess.run(
    [sys.executable, "get_logs.py", "--save"],
    capture_output=True,
    text=True,
    timeout=30
)

if result.returncode == 0:
    # 读取崩溃报告
    from pathlib import Path
    crash_files = sorted(Path("logs").glob("crash_report_*.txt"))
    if crash_files:
        crash_content = crash_files[-1].read_text(encoding="utf-8")
        
        # 分析崩溃类型
        if "IllegalStateException" in crash_content:
            print("崩溃类型: IllegalStateException（状态异常）")
            print("可能原因: 数据库未初始化或对象状态不正确")
        elif "NullPointerException" in crash_content:
            print("崩溃类型: NullPointerException（空指针异常）")
            print("可能原因: 对象未初始化或空值未检查")
        elif "SQLiteException" in crash_content:
            print("崩溃类型: SQLiteException（数据库异常）")
            print("可能原因: 数据库文件损坏或 SQL 语句错误")
```

### 场景3：监控应用状态

```python
import subprocess
import sys
import time

def monitor_app_logs(interval=60):
    """定期检查应用日志"""
    while True:
        result = subprocess.run(
            [sys.executable, "get_logs.py"],
            capture_output=True,
            text=True,
            timeout=30
        )
        
        if result.returncode == 0:
            logs = result.stdout
            error_count = logs.count("[ERROR]")
            warn_count = logs.count("[WARN]")
            
            print(f"[{time.strftime('%Y-%m-%d %H:%M:%S')}] "
                  f"错误: {error_count}, 警告: {warn_count}")
            
            if error_count > 0:
                print("⚠️ 发现错误，需要关注！")
        
        time.sleep(interval)

# 每60秒检查一次
# monitor_app_logs(60)
```

## 注意事项

1. **前置要求**：
   - ADB 工具必须可用
   - 设备必须通过 USB 连接
   - 应用必须已安装

2. **超时设置**：建议设置合理的超时时间（如 30 秒）

3. **错误处理**：始终检查返回码，处理可能的错误

4. **日志文件**：保存的日志文件会自动添加到 `.gitignore`，不会被提交到版本控制

## 故障排除

如果工具无法工作，检查：
1. ADB 是否在 PATH 中：`which adb`
2. 设备是否连接：`adb devices`
3. 应用是否安装：`adb shell pm list packages | grep com.handnote.app`

