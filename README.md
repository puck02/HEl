# HElDairy — AI 私人健康管家

简体中文说明。该项目实现一个面向 Android（Android 10+）的“AI 私人健康管家”原型：连续式每日健康问诊对话、规则驱动的跟进问题、以及可选的 DeepSeek AI 建议生成功能。

核心特性
- Kotlin + Jetpack Compose (Material 3)
- Room 持久化 + ViewModel + StateFlow
- Retrofit/OkHttp + kotlinx.serialization（DeepSeek 与网络层）
- 可配置的 DeepSeek AI 集成（离线可用、出现异常时回退）
- 本地导出/导入（JSON 备份 + CSV 医生导出）

仓库结构（部分）
- [app](app)
- [doc/requirements.md](doc/requirements.md)
- [dev_logs/2026-01-13.md](dev_logs/2026-01-13.md)

快速开始（开发者）

先决条件
- Android Studio（推荐）
- JDK 17
- Android SDK（设置 `ANDROID_HOME` 指向你的 SDK 目录）

示例环境设置（macOS / zsh）
```bash
export ANDROID_HOME=/Users/you/Library/Android/sdk
export PATH=$PATH:$ANDROID_HOME/platform-tools
source ~/.zshrc
```

构建与运行
```bash
# 检查并构建（单模块/单次构建）
./gradlew clean build

# 生成调试 APK（M6 要求）
./gradlew assembleDebug

# 运行单元测试
./gradlew test
```

AI Key 与隐私
- DeepSeek API Key：应用提供设置界面以粘贴/管理 Key（见项目需求）。Key 建议保存在加密的本地存储或偏好设置中，可在设置中禁用 AI 功能而不删除数据。
- 严格遵守本地优先策略：健康数据不自动同步到云；如 DeepSeek 不可用，界面会显示“AI 建议暂时不可用”，并跳过当次建议生成。

导出 / 导入
- 支持通过 Storage Access Framework 导出 JSON 备份（包含 `schemaVersion` 与完整记录）以及 CSV 医生导出。
- 导入 JSON 时会在验证 schema 后进行覆盖式恢复。

里程碑与交付（参见详细需求）
- 本项目按照 M0–M6 里程碑开发；每完成一项里程碑请运行 `./gradlew build` 验证。
- 详见 [doc/requirements.md](doc/requirements.md)

开发提示与约束
- 网络层：使用 Retrofit + OkHttp 拦截器注入鉴权头并记录请求（生产中应屏蔽或脱敏日志）。
- 规则树：症状跟进以可序列化的数据结构（sealed class 或 JSON 资产）实现，便于扩展而不改动业务逻辑。
- 测试优先：为自适应触发器与 JSON 验证器编写单元测试。

贡献
- 请参考项目代码风格并在提交前运行 `./gradlew build`。

参考与文档
- 需求说明：[doc/requirements.md](doc/requirements.md)
- 开发日志：[dev_logs/2026-01-13.md](dev_logs/2026-01-13.md)

许可
- 参见仓库根目录的 `LICENSE` 文件。

——
README 文件已根据仓库内需求与开发说明编写。如需加入更多使用示例、屏幕截图或发布说明，请告诉我要补充的内容。
