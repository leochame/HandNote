# Gmail + AI 功能设计文档

## 功能概述

为 HandNote APP 新增 Gmail 邮件集成与 AI 智能分析功能：

1. **邮件浏览**：支持翻阅 Gmail 未读邮件列表
2. **AI 每日总结**：利用 Gemini AI 自动总结今日未读邮件内容
3. **面试智能识别**：AI 检测邮件中的面试安排，自动生成提醒和待办事项

## 技术架构

### 依赖组件

| 组件 | 用途 |
|------|------|
| Google Sign-In | Gmail API OAuth 认证 |
| Gmail REST API | 读取邮件列表和内容 |
| Google Gemini API | AI 总结与面试信息提取 |

### 数据流

```
用户登录 Gmail → 获取 OAuth Token → 调用 Gmail API 获取未读邮件
                                    ↓
                    邮件内容 → Gemini API → 总结 + 面试检测
                                    ↓
                    面试信息 → 创建 TaskRecord → AlarmManager 提醒
```

### 新增数据模型

- **TaskRecord.sourceType** 新增值：`"gmail_interview"` - 来自 Gmail 的面试提醒
- **SharedPreferences** 存储：Gemini API Key、Gmail 登录状态

## 用户配置

1. **设置页**：添加「Gmail 集成」入口
   - 登录/登出 Gmail 账户
   - 配置 Gemini API Key（从 https://ai.google.dev 获取）

2. **Gmail 标签页**：新增底部导航「邮件」
   - 邮件列表（今日未读）
   - AI 总结卡片
   - 面试待办列表

## 隐私与安全

- API Key 仅存储在本地 SharedPreferences
- 邮件数据仅用于 AI 分析，不持久化存储
- OAuth Token 由 Google 安全管理

