# HandNote - 效率手账 APP

一个纯本地的效率手账 Android 应用，使用 Kotlin、Jetpack Compose、Room Database、ViewModel 和 Coroutines 构建。

## 技术栈

- **语言**: Kotlin
- **UI**: Jetpack Compose
- **数据库**: Room Database
- **架构**: MVVM (ViewModel)
- **异步**: Kotlin Coroutines

## 项目结构

```
app/src/main/java/com/handnote/app/
├── data/
│   ├── dao/              # Room DAO 接口
│   │   ├── ShiftRuleDao.kt
│   │   ├── AnniversaryDao.kt
│   │   ├── TaskRecordDao.kt
│   │   ├── PostDao.kt
│   │   └── HolidayDao.kt
│   ├── entity/           # Room Entity 数据模型
│   │   ├── ShiftRule.kt
│   │   ├── Anniversary.kt
│   │   ├── TaskRecord.kt
│   │   ├── Post.kt
│   │   └── Holiday.kt
│   ├── database/         # Room Database
│   │   └── AppDatabase.kt
│   └── repository/       # 数据仓库层
│       └── AppRepository.kt
├── ui/
│   ├── screens/          # 各个页面 Screen
│   │   ├── CalendarScreen.kt
│   │   ├── FeedScreen.kt
│   │   ├── ConfigScreen.kt
│   │   └── SettingsScreen.kt
│   ├── navigation/       # 导航配置
│   │   └── Screen.kt
│   ├── viewmodel/        # ViewModel
│   │   ├── MainViewModel.kt
│   │   └── ViewModelFactory.kt
│   ├── theme/            # 主题配置
│   │   └── Theme.kt
│   └── MainScreen.kt     # 主界面
└── MainActivity.kt       # 主 Activity
```

## 数据库表结构

### 1. ShiftRule (排班规则)
- `id`: Long (主键，自增)
- `title`: String
- `startDate`: Long (时间戳)
- `cycleDays`: Int
- `shiftConfig`: String (JSON 字符串)
- `skipHoliday`: Boolean
- `defaultReminderLevel`: Int

### 2. Anniversary (纪念日)
- `id`: Long (主键，自增)
- `title`: String
- `targetDate`: String (日期格式: "YYYY-MM-DD")
- `reminderLevel`: Int
- `reminderTime`: Long? (时间戳，可为空)

### 3. TaskRecord (每日任务)
- `id`: Long (主键，自增)
- `sourceType`: String
- `sourceId`: Long
- `targetDate`: String (日期格式: "YYYY-MM-DD")
- `triggerTimestamp`: Long
- `reminderLevel`: Int
- `status`: String
- `targetPkgName`: String? (可为空)

### 4. Post (帖子)
- `id`: Long (主键，自增)
- `createTime`: Long (时间戳)
- `content`: String
- `imagePaths`: String? (JSON 字符串，可为空)
- `linkedTaskIds`: String? (JSON 字符串，可为空)

### 5. Holiday (节假日)
- `date`: String (主键，日期格式: "YYYY-MM-DD")
- `type`: String
- `name`: String

## 功能模块

### 底部导航栏
应用包含 4 个主要 Tab：
1. **日程 (Calendar)**: 显示日程相关功能
2. **沉淀 (Feed)**: 显示帖子/记录
3. **配置 (Config)**: 应用配置
4. **设置 (Settings)**: 应用设置

## 构建和运行

### 前置要求

- **Android Studio**: Hedgehog (2023.1.1) 或更高版本
  - 下载地址: https://developer.android.com/studio
- **JDK**: 17 或更高版本（Android Studio 自带）
- **Android SDK**: API 34 (Android 14)

### 快速开始

#### 方法一：使用 Android Studio（推荐）

1. **打开项目**
   - 启动 Android Studio
   - `File` → `Open` → 选择项目目录
   - 等待 Gradle 同步完成（首次可能需要几分钟下载依赖）

2. **运行应用**
   - 点击工具栏的绿色运行按钮 ▶️
   - 或按快捷键 `Shift + F10` (Windows/Linux) / `Ctrl + R` (Mac)
   - 选择设备（模拟器或真机）

#### 方法二：使用命令行

```bash
# macOS/Linux
./gradlew build          # 构建项目
./gradlew installDebug   # 安装到设备

# Windows
gradlew.bat build
gradlew.bat installDebug
```

### 创建 Android 模拟器

如果没有真机设备：

1. `Tools` → `Device Manager` → `Create Device`
2. 选择设备型号（推荐：Pixel 5）
3. 选择系统镜像（推荐：API 34, Android 14）
4. 完成创建并启动

详细说明请查看 [运行指南.md](./运行指南.md)

## 技术文档

- **[技术架构与开发方案.md](./技术架构与开发方案.md)** - 完整的技术架构设计文档，包含：
  - 业务闭环与系统总览
  - 核心技术栈与架构模式
  - 分级提醒引擎实现方案
  - 动态排班算法详解
  - 数据模型设计
  - UI 视图层架构
  - 工程化与生产特性

## 依赖版本

- Compose BOM: 2023.10.01
- Room: 2.6.1
- Kotlin: 1.9.20
- Android Gradle Plugin: 8.2.0
- Compile SDK: 34
- Min SDK: 24
- Target SDK: 34

