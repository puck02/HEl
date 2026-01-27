
# HElDairy — AI 私人健康管家

简介
---
HElDairy 是一个面向 Android（Android 10+）的原型应用，目标为提供“生活管家”式的每日健康对话：记录日常指标、按规则触发跟进问题，并在可用时调用 DeepSeek AI 生成个性化建议。项目以本地优先为原则，用户数据默认仅保存在设备上。

主要特性
---
- 连续式每日问诊对话流程（Step 0–Step 3，参见需求文档）
- 规则驱动的自适应跟进问题（可序列化的规则树，便于扩展）
- Kotlin + Jetpack Compose（Material 3）UI
- Room 持久化层 + ViewModel + StateFlow
- DeepSeek AI 可选集成（通过 Retrofit + OkHttp + kotlinx.serialization）
- 本地导出/导入：JSON 备份（含 schemaVersion）与 CSV 医生导出

技术栈
---
- 语言：Kotlin
# HElDairy — AI 私人健康管家

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE) [![Kotlin](https://img.shields.io/badge/Kotlin-1.9-blue.svg)](https://kotlinlang.org/) [![Android](https://img.shields.io/badge/Android-10%2B-brightgreen.svg)](https://developer.android.com)

简介
---
HElDairy 是一个面向 Android（Android 10+）的原型应用，提供“生活管家”式的每日健康对话：记录日常指标、按规则触发跟进问题，并在可用时调用 DeepSeek AI 生成非诊断性的生活方式建议。应用以本地优先为原则，用户健康数据默认仅保存在设备上。

目录
---
- [主要特性](#主要特性)
- [技术栈](#技术栈)
- [文件结构](#文件结构)
- [快速开始](#快速开始)
- [构建与测试](#构建与测试)
- [配置与运行说明](#配置与运行说明)
- [导出 / 导入](#导出--导入)
- [里程碑与交付](#里程碑与交付)
- [贡献](#贡献)
- [许可](#许可)
- [鸣谢](#鸣谢)

主要特性
---
- 连续式每日问诊对话（Step 0–Step 3，见需求）
- 规则驱动的自适应跟进问题（可序列化规则树）
- Kotlin + Jetpack Compose（Material 3）UI
- Room 持久化 + ViewModel + StateFlow
- DeepSeek AI 可选集成（Retrofit + OkHttp + kotlinx.serialization）
- 本地导出/导入：JSON 备份（含 schemaVersion）与 CSV 医生导出

技术栈
---
- 语言：Kotlin
- UI：Jetpack Compose + Material 3
- 持久化：Room
- 网络：Retrofit + OkHttp
- 序列化：kotlinx.serialization

文件结构（示例）
---
```
HEl/
├── app/
├── doc/
│   └── requirements.md
├── dev_logs/
├── README.md
└── LICENSE
```

快速开始
---
先决条件
- Android Studio（推荐）
- JDK 17
- Android SDK（确保 `ANDROID_HOME` 指向 SDK 路径）

示例（macOS / zsh）
```bash
export ANDROID_HOME=/Users/you/Library/Android/sdk
export PATH=$PATH:$ANDROID_HOME/platform-tools:$PATH
source ~/.zshrc
```

构建与测试
---
```bash
# 清理并构建
./gradlew clean build

# 生成调试 APK（用于 M6 交付）
./gradlew assembleDebug

# 运行单元测试
./gradlew test
```

配置与运行说明
---
- DeepSeek API Key：在应用设置中粘贴用户的 DeepSeek Key。建议使用加密的本地存储保存 Key，并提供开关以禁用 AI 功能而不删除本地数据。
- 离线优先：核心问诊流与数据记录在离线状态下可用；当 DeepSeek 或网络不可用时，AI 建议步骤会被跳过并向用户反馈“AI 建议暂时不可用”。

调试示例
---
```bash
./gradlew installDebug
adb logcat -s HElDairy:V
```

导出 / 导入
---
- 支持通过 Storage Access Framework 导出完整 JSON 备份（包含 `schemaVersion`）以及 CSV 导出。
- 导入 JSON 前会进行 schema 验证；验证失败将返回错误，避免伪造或不完整的数据导入。


贡献
---
1. Fork 本仓库
2. 创建分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改并推送 (`git commit -m "Add feature" && git push`)
4. 打开 Pull Request

请在提交重大变更前先在 Issue 中讨论设计。

许可
---
本项目采用 MIT 许可 — 详见仓库根目录的 `LICENSE` 文件。

鸣谢
---

