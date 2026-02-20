# Agent 日志获取工具

## 概述

这个工具允许 Agent 直接获取 HandNote 应用的日志，无需用户手动操作。

## 使用方法

### 方法1：直接获取日志内容（推荐）

Agent 可以直接运行脚本获取日志内容：

```bash
python3 get_logs.py
```

这会直接输出最新的日志内容，包括：
- 崩溃报告（如果有）
- 应用日志
- 错误和警告统计

### 方法2：保存日志到工作区

将日志保存到工作区的 `logs/` 目录，方便后续分析：

```bash
python3 get_logs.py --save
```

保存的文件：
- `logs/crash_report_YYYYMMDD_HHMMSS.txt` - 崩溃报告
- `logs/app_log_YYYYMMDD_HHMMSS.txt` - 应用日志

保存后，Agent 可以直接读取这些文件进行分析。

## 前置要求

1. **ADB 工具**：需要安装 Android SDK Platform Tools
2. **设备连接**：Android 设备需要通过 USB 连接并启用 USB 调试
3. **应用安装**：HandNote 应用必须已安装在设备上

## 在代码中使用

Agent 可以在代码中直接调用这个工具：

```python
import subprocess
import sys

# 获取日志内容
result = subprocess.run(
    [sys.executable, "get_logs.py"],
    capture_output=True,
    text=True
)
logs = result.stdout

# 或者保存到工作区
result = subprocess.run(
    [sys.executable, "get_logs.py", "--save"],
    capture_output=True,
    text=True
)
```

## 日志文件位置

### 设备上的位置
- 崩溃报告：`/data/data/com.handnote.app/files/crash_report.txt`
- 应用日志：`/data/data/com.handnote.app/files/logs/app_YYYY-MM-DD.log`

### 工作区位置（保存后）
- `logs/crash_report_*.txt`
- `logs/app_log_*.txt`

## 故障排除

### ADB 未找到
```
❌ 错误: 未找到 ADB 工具
```
**解决方案**：安装 Android SDK Platform Tools 并添加到 PATH

### 设备未连接
```
❌ 错误: 未检测到已连接的 Android 设备
```
**解决方案**：
1. 确保设备通过 USB 连接
2. 启用 USB 调试
3. 授权此计算机进行 USB 调试

### 应用未安装
```
❌ 错误: 未找到 com.handnote.app 应用
```
**解决方案**：确保应用已安装在设备上

## 示例输出

```
==================================================
崩溃报告
==================================================
========================================
CRASH REPORT
========================================
Timestamp: 2024-01-15 10:30:45.789
Exception: java.lang.IllegalStateException
Message: Database initialization failed
...

==================================================
应用日志
==================================================
[2024-01-15 10:30:45.123] [DEBUG] [MainActivity] MainActivity.onCreate started
[2024-01-15 10:30:45.456] [INFO] [MainActivity] Database initialized successfully
[2024-01-15 10:30:45.789] [ERROR] [MainActivity] Failed to initialize app
...

==================================================
统计: 5 个错误, 2 个警告
==================================================
```

## 注意事项

1. 日志文件会自动清理，最多保留 10 个日志文件
2. 单个日志文件最大 5MB，超过后会自动创建新文件
3. 如果应用崩溃，崩溃报告会自动保存
4. 导出的日志文件包含所有应用日志（包括崩溃前的日志）

