# HElDairy — AI 私人健康管家

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE) 
[![Kotlin](https://img.shields.io/badge/Kotlin-1.9-blue.svg)](https://kotlinlang.org/) 
[![Android](https://img.shields.io/badge/Android-10%2B-brightgreen.svg)](https://developer.android.com)
[![GitHub release](https://img.shields.io/github/v/release/puck02/HEl?include_prereleases)](https://github.com/puck02/HEl/releases)

## 📥 下载安装

**最新版本**: [点击这里下载最新APK](https://github.com/puck02/HEl/releases/latest)

### 安装说明
1. 下载对应的 APK 文件（推荐 Release 版本）
2. 在 Android 设备上启用「允许安装未知来源应用」
3. 打开下载的 APK 文件进行安装
4. 首次使用请在设置中配置 **智能体服务器** 或 **DeepSeek API Key**

---

## 简介

HElDairy 是一款面向 Android 10+ 的 AI 健康管理应用，提供「生活管家」式的每日健康对话：
- 记录日常健康指标（睡眠、情绪、饮食、运动等）
- 规则驱动的自适应跟进问题
- AI 生成的个性化健康建议（非医疗诊断）
- 用药提醒与管理
- 每周健康洞察报告

**本地优先**：用户健康数据默认仅保存在设备上，可选择性同步到自托管的智能体服务器。

---

## 目录

- [主要特性](#主要特性)
- [架构概览](#架构概览)
- [智能体集成](#智能体集成)
- [技术栈](#技术栈)
- [快速开始](#快速开始)
- [配置说明](#配置说明)
- [构建与测试](#构建与测试)
- [贡献](#贡献)
- [许可](#许可)

---

## 主要特性

| 功能 | 说明 |
|------|------|
| 📋 每日问诊对话 | Step 0–3 连续式问答，自动跟进 |
| 🤖 AI 健康建议 | DeepSeek / 智能体双路径，自动回退 |
| 💊 用药管理 | 用药提醒、NLP 解析处方、疗程追踪 |
| 📊 周报洞察 | 每周自动生成健康趋势分析 |
| 🔄 数据同步 | 增量同步到自托管服务器（可选） |
| 📤 导出/导入 | JSON 备份 + CSV 医生报告导出 |
| 🌐 多语言 | 中文 / English |

---

## 架构概览

```
┌────────────────────────────────────────────────────────────────┐
│                     HElDairy Android App                       │
│                                                                │
│  ┌──────────┐   ┌─────────────┐   ┌─────────────────────────┐ │
│  │ Compose  │──▶│ ViewModel   │──▶│ Coordinator             │ │
│  │ UI       │   │ (StateFlow) │   │ (Agent-First + Fallback)│ │
│  └──────────┘   └─────────────┘   └───────────┬─────────────┘ │
│                                               │               │
│                         ┌─────────────────────┼───────────────┤
│                         │                     │               │
│                         ▼                     ▼               │
│               ┌─────────────────┐   ┌─────────────────┐       │
│               │ AgentClient     │   │ DeepSeekClient  │       │
│               │ (智能体优先)    │   │ (回退方案)      │       │
│               └────────┬────────┘   └─────────────────┘       │
│                        │                                      │
└────────────────────────┼──────────────────────────────────────┘
                         │ HTTPS
                         ▼
┌────────────────────────────────────────────────────────────────┐
│              hel-agent 智能体后端 (自托管)                      │
│                                                                │
│  ┌─────────┐   ┌──────────┐   ┌────────────┐   ┌────────────┐ │
│  │ FastAPI │──▶│ LangGraph│──▶│ Multi-Agent│──▶│ LLM Router │ │
│  │ REST    │   │ Workflow │   │ Orchestrator│  │GLM/DeepSeek│ │
│  └─────────┘   └──────────┘   └────────────┘   └────────────┘ │
│                                                                │
│  ┌─────────────────────────────────────────────────────────┐  │
│  │ PostgreSQL + pgvector │ Redis │ Qdrant (RAG)            │  │
│  └─────────────────────────────────────────────────────────┘  │
└────────────────────────────────────────────────────────────────┘
```

---

## 智能体集成

### 工作原理

HElDairy v1.1+ 支持连接到 **hel-agent** 智能体后端，提供更强大的 AI 能力：

1. **Agent-First 策略**：App 优先尝试智能体服务器
2. **自动回退**：若智能体不可用，自动回退到设备端 DeepSeek API
3. **增量同步**：健康数据可选择性同步到服务器，支持跨设备访问

### 服务器对接配置

**生产环境服务器**：
- **地址**：`http://167.172.90.251:8000`
- **后端框架**：FastAPI
- **协议**：HTTP REST API + JWT 认证

#### 在 App 中配置

1. 打开 **设置** 页面
2. 找到 **🤖 智能体 Agent** 区域
3. 启用「启用智能体」开关
4. 输入服务器地址：`http://167.172.90.251:8000`
5. 点击「保存」
6. 注册账号或登录已有账号
7. （可选）启用「自动同步健康数据」

#### 网络连接流程

```
Android App                        hel-agent Server (FastAPI)
     │                                    │
     │──── POST /auth/register ──────────▶│  注册新用户
     │◀─── { access_token, refresh } ─────│
     │                                    │
     │──── POST /auth/login ─────────────▶│  登录
     │◀─── { access_token, refresh } ─────│
     │                                    │
     │──── GET /health ──────────────────▶│  健康检查
     │◀─── { status: "healthy" } ─────────│
     │                                    │
     │──── POST /health/daily-advice ────▶│  获取每日建议
     │     Authorization: Bearer <token>  │
     │◀─── { red_flags, observations } ───│
     │                                    │
     │──── POST /health/sync/upload ─────▶│  同步健康数据
     │     { health_entries, meds... }    │
     │◀─── { synced_count: 42 } ──────────│
```

#### Token 刷新机制

- **Access Token** 有效期：30 分钟
- **Refresh Token** 有效期：7 天
- App 在收到 401 响应时自动尝试刷新 Token
- 刷新失败则自动登出，需重新登录

### API 端点一览

| 端点 | 方法 | 说明 |
|------|------|------|
| `/auth/register` | POST | 注册新用户 |
| `/auth/login` | POST | 登录获取 Token |
| `/auth/refresh` | POST | 刷新 Access Token |
| `/health` | GET | 服务器健康检查 |
| `/health/daily-advice` | POST | 获取每日健康建议 |
| `/health/follow-up` | POST | 获取跟进问题 |
| `/health/weekly-insight` | POST | 获取周报洞察 |
| `/health/sync/upload` | POST | 上传健康数据 |
| `/health/sync/status` | GET | 查询同步状态 |
| `/medication/parse-nlp` | POST | NLP 解析处方文本 |
| `/chat` | POST | 通用对话（带 RAG） |

---

## 技术栈

### Android 客户端

| 组件 | 技术 |
|------|------|
| 语言 | Kotlin 1.9 |
| UI | Jetpack Compose + Material 3 |
| 架构 | MVVM + Repository + Coordinator |
| 持久化 | Room 2.6 (SQLite) |
| 网络 | Retrofit 2.11 + OkHttp 4.12 |
| 序列化 | kotlinx.serialization |
| 后台任务 | WorkManager |
| 偏好存储 | DataStore + EncryptedSharedPreferences |

### hel-agent 后端

| 组件 | 技术 |
|------|------|
| 框架 | FastAPI + Uvicorn |
| Agent 编排 | LangGraph ≥ 0.2.40 |
| LLM 路由 | LiteLLM (GLM → DeepSeek → OpenAI) |
| 数据库 | PostgreSQL 16 + pgvector |
| 缓存 | Redis 7 |
| 向量检索 | Qdrant |
| 认证 | JWT (PyJWT + passlib) |
| 部署 | Docker + Docker Compose |

---

## 快速开始

### 前置条件

- Android Studio Hedgehog+
- JDK 17
- Android SDK（`ANDROID_HOME` 环境变量）

### 克隆与构建

```bash
git clone https://github.com/puck02/HEl.git
cd HEl

# 构建 Debug APK
./gradlew assembleDebug

# 安装到设备
./gradlew installDebug
```

---

## 配置说明

### 方式一：智能体模式（推荐）

1. 部署 hel-agent 到服务器（[HEI-agent GitHub](https://github.com/puck02/HEI-agent)）
2. 在 App 设置中配置服务器地址
3. 注册/登录账号
4. 启用数据同步

### 方式二：纯本地模式（DeepSeek）

1. 获取 DeepSeek API Key：[platform.deepseek.com](https://platform.deepseek.com/)
2. 在 App 设置 → API Key 中粘贴
3. 关闭「启用智能体」开关

### 离线优先

- 核心问诊流程和数据记录在离线时可用
- AI 建议在网络不可用时跳过
- 同步任务在网络恢复后自动重试

---

## 构建与测试

```bash
# 清理构建
./gradlew clean build

# 生成 Release APK（需配置签名）
./gradlew assembleRelease

# 运行单元测试
./gradlew test

# 查看日志
adb logcat -s HElDairy:V
```

---

## 文件结构

```
HEl/
├── app/src/main/java/com/heldairy/
│   ├── core/
│   │   ├── data/           # Coordinator（Agent-First 路径）
│   │   ├── database/       # Room 实体 & DAO
│   │   ├── di/             # 依赖注入 (AppContainer)
│   │   ├── network/
│   │   │   ├── agent/      # AgentClient, AgentApi, AuthInterceptor
│   │   │   └── deepseek/   # DeepSeekClient
│   │   ├── preferences/    # DataStore (AgentPreferencesStore)
│   │   └── worker/         # WorkManager (DataSyncWorker)
│   ├── feature/
│   │   ├── home/           # 首页
│   │   ├── report/         # 日报问诊
│   │   ├── medication/     # 用药管理
│   │   ├── insight/        # 周报洞察
│   │   └── settings/       # 设置（含 Agent 配置）
│   └── ui/theme/           # Material 3 主题
├── doc/                    # 需求与设计文档
├── dev_logs/               # 开发日志
└── README.md
```

---

## 贡献

1. Fork 本仓库
2. 创建分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m "Add feature"`)
4. 推送并创建 Pull Request

重大变更请先在 Issue 中讨论。

---

## 许可

本项目采用 MIT 许可 — 详见 [LICENSE](LICENSE) 文件。

---

## 相关项目

- **[HEI-agent](https://github.com/puck02/HEI-agent)** — 智能体后端服务（FastAPI + LangGraph）

---

## 鸣谢

- [DeepSeek](https://deepseek.com/) — AI 模型
- [LangGraph](https://github.com/langchain-ai/langgraph) — Agent 编排框架
- [Jetpack Compose](https://developer.android.com/jetpack/compose) — 现代 Android UI

