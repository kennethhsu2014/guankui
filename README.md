# 管窥 - 通知消息记录工具

一个轻量级的 Android 通知消息记录应用。

## 功能

- ✅ 自动捕获通知栏消息
- ✅ 记录 APP 名称、时间、内容
- ✅ 按时间排序查看历史消息
- ✅ 按 APP 名称筛选
- ✅ 一键清空历史记录
- ✅ 本地存储，无需后端

## 系统要求

- Android 8.0+ (API 26+)
- 需要开启通知监听权限

## 构建方式

### 方式一：GitHub Actions（推荐）

1. 将此项目推送到 GitHub
2. Actions 会自动编译 APK
3. 在 Actions 页面下载生成的 APK

### 方式二：本地编译

需要安装：
- JDK 17+
- Android SDK

```bash
# 设置环境变量
export ANDROID_HOME=/path/to/android/sdk

# 编译
./gradlew assembleRelease

# APK 输出位置
app/build/outputs/apk/release/app-release-unsigned.apk
```

## 使用说明

1. 安装 APK
2. 打开 APP，授予通知监听权限
3. 开始自动记录通知消息

## 技术栈

- Kotlin
- AndroidX
- Room Database
- Material Design

## 注意事项

- 需要手动开启通知监听权限（设置 → 通知 → 通知监听）
- 数据存储在本地，卸载 APP 会清除所有数据
- 不会记录管窥 APP 自身的通知
