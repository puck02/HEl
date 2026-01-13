你是我的编码代理。请在一个全新工作区创建一个 Android App（Android 10+，仅个人使用，可生成 APK），这是一个“AI 私人健康管家”。

核心体验目标（非常重要）：
- 用户每天不是“填表”，而是进行一次「对话式日报」
- AI 会通过提问引导用户回答（选择/简答）
- AI 会根据当天回答 + 最近历史，动态决定“是否追问、追问什么”
- 每天日报结束后，AI 会立刻给出“当天的生活管家式建议”
- 当用户状态良好时，问题尽量少；不舒服时，问题更细致（自适应）

总体架构：
- 本地数据库（Room）= 长期记忆（所有原始回答）
- AI（DeepSeek API）= 理解、追问、建议生成
- 不使用云存储（除 AI 接口）
- 不做医疗诊断、不改药，只给生活方式建议

技术栈：
- Kotlin
- Jetpack Compose + Material 3
- Room (SQLite)
- ViewModel + StateFlow
- Retrofit / OkHttp（DeepSeek API）
- kotlinx.serialization（JSON）
- Storage Access Framework（导入/导出迁移）
- 使用 Material Icons，不依赖外部图片素材

====================
一、日报对话系统设计（重点）
====================

日报流程（每天一次）：

Step 0：开场（固定）
- 问题1：今天整体感觉如何？
  选项：很好 / 还行 / 有点不舒服 / 很难受
- 问题2：今天你更想重点关注哪个？
  选项：头痛 / 颈肩腰 / 胃 / 鼻咽 / 膝盖 / 情绪 / 睡眠 / 经期 / 没有特别

Step 1：基础问题（固定问题池，全部为选择或滑条）
- 睡眠时长（<6 / 6-7 / 7-8 / >8）
- 午睡（无 / <30 / 30-60 / >60）
- 步数（<3000 / 3-6k / 6-10k / >10k）
- 头痛强度（0-10）
- 颈肩腰痛强度（0-10）
- 胃不适强度（0-10）
- 鼻咽不适强度（0-10）
- 膝盖不适强度（0-10）
- 情绪烦躁（0-10）
- 受凉（是/否）
- 用药是否按时（是/否）
- 经期状态（经期 / 非经期 / 异常）
- 今日补充说明（简短文本，可跳过）

目标：1～2 分钟完成基础部分。

Step 2：自适应追问（关键）
- 根据以下因素，决定是否进入追问：
  - 当天某症状 >=6
  - 或最近 3 天同一症状上升
  - 或用户在 Step 0 表示“很难受”
- 追问来源：
  A) 本地规则追问树（优先，稳定）
     - 例如：
       - 头痛：性质 / 持续时间 / 是否久坐 / 是否畏光恶心
       - 胃：反酸/胀/痛 / 晚餐时间 / 刺激性饮食
       - 膝盖：受凉 / 活动后加重 / 肿胀感
  B) AI 补充追问（最多 2 个）
     - AI 基于：今天基础回答 + 最近 7 天游摘要
     - 输出必须是结构化 JSON（问题文本 + 类型 + 选项）
     - 不允许诊断性问题、不允许开放式长问

Step 3：当日建议生成（必须用 AI）
- 输入：
  - 今天全部回答（含追问）
  - 最近 7 天游结构化摘要
- AI 输出固定 JSON 结构：
  - observations[]：今天的观察（1-3 条）
  - actions[]：今天/明天的具体建议（最多 3 条）
  - tomorrow_focus[]：明天重点关注的 1-2 件事
  - red_flags[]：仅提醒“如出现请就医”，不诊断

UI 渲染建议为“生活管家语气”：温柔、理性、不夸张。

====================
二、数据与记忆
====================

- Room 保存：
  - 每个问题的答案（结构化）
  - 追问问题与回答
  - AI 当日建议（缓存，便于回看）
- 本地生成摘要：
  - 最近 7 天 / 30 天的均值、趋势、异常次数
- AI 永远只读“摘要 + 当天回答”，不直接读数据库

====================
三、DeepSeek API 集成规则
====================

- 使用 DeepSeek Chat Completions API（OpenAI 兼容）
- App 内设置页提供：
  - API Key 输入框（用户填写）
  - 开关：启用/停用 AI 管家
- API Key 存储在本地（SharedPreferences / Encrypted）
- 网络失败：
  - 明确提示“AI 建议暂时不可用”
  - 不生成假建议
- AI 输出必须校验 JSON 结构，失败则提示重试

====================
四、迁移与导出
====================

- JSON 备份导出（覆盖恢复）：
  - schemaVersion
  - records（所有日报与回答）
- CSV 导出（用于查看/给医生）
- 使用 Storage Access Framework

====================
五、vibe coding 执行规则（必须遵守）
====================

- 采用里程碑开发，一次只做一个里程碑
- 每个里程碑完成必须通过 ./gradlew build
- 完成一个里程碑后停下来，告诉我如何验证，再进入下一个

里程碑：

M0：工程初始化
- 空壳 App + 4 Tab
- ./gradlew build 通过

M1：基础日报对话（无 AI）
- Step 0 + Step 1
- 回答可保存到 Room
- ./gradlew build 通过

M2：AI 当日建议
- 本地摘要生成
- 调用 DeepSeek 生成当日建议
- JSON 校验 + 失败处理
- ./gradlew build 通过

M3：规则追问树
- 症状触发追问
- UI 可动态插入问题
- ./gradlew build 通过

M4：AI 补充追问
- AI 返回 1-2 个补充问题（结构化）
- 校验并渲染
- ./gradlew build 通过

M5：迁移与导出
- JSON 覆盖恢复
- CSV 导出
- ./gradlew build 通过

M6：打包交付
- ./gradlew assembleDebug
- README：安装、迁移、API Key 设置说明

====================
六、用药管理（新增，重点：结构化疗程 + 自然语言事件 + AI 辅助解析）
====================

目标：
- 提供“用药记录/用药管理”能力，支持用户输入用药的开始/结束时间、频率、是否中断等信息
- 用户可在任意时间进入用药管理页，输入一段自然语言描述（例如：“我最近感冒了，XXX药就先停了，先吃感冒药YYY。”）
- App 通过 DeepSeek 将自然语言解析为结构化变更草稿（新增药品/开始疗程/暂停/结束等），并要求用户确认后落库
- 用药信息会随“日报”一并提供给 AI，用于更个性化的追问与建议（但不做诊断、不改药）

数据模型（Room）：

1) 药品库 Med
- id: Long (PK)
- name: String（药名，显示用）
- aliases: String?（可选，逗号分隔；用于匹配用户输入）
- createdAt: Long
- updatedAt: Long
- note: String?（用户自填备注，可选）
- infoSummary: String?（药品介绍/用法用量/注意事项/不良反应的“摘要”，可由 AI/OCR 生成，最终可编辑）
- imageUri: String?（可选，拍照/相册导入的图片 URI；MVP 可不做）

2) 用药疗程 MedCourse（对应“开始时间/结束时间/频率/中断”）
- id: Long (PK)
- medId: Long (FK -> Med.id)
- startDate: LocalDate
- endDate: LocalDate?（空 = 仍在吃）
- status: String（active / paused / ended）
- frequencyText: String（例如：每日3次 / 每日1次 / 按需；先用文本，避免过度结构化）
- doseText: String?（例如：每次1片 / 2片 / xx ml）
- timeHints: String?（例如：早 / 中 / 晚 / 睡前）
- createdAt: Long
- updatedAt: Long

3) 用药事件流 MedEvent（对应“随手输入的一段话”，可追溯、可用于 AI）
- id: Long (PK)
- createdAt: Long
- rawText: String（用户原文）
- detectedMedNamesJson: String?（AI 识别到的药名候选列表，JSON 字符串）
- proposedActionsJson: String?（AI 提供的结构化变更草稿，JSON 字符串）
- confirmedActionsJson: String?（用户确认后的变更，JSON 字符串）
- applyResult: String?（success / failed + message）

药品库展示规则（UI）：
- 用药管理页展示“所有药品”
- 若该药存在至少一个 status = active 的 MedCourse，则该药显示为绿色（正在吃）
- 若该药没有 active 疗程（全部 ended / paused 或无疗程），显示为灰色（已停用/未在用）
- 点击药品进入详情页：
  - 显示药品介绍、用法用量、注意事项、不良反应（infoSummary，可编辑）
  - 显示当前疗程（若有 active / paused）
  - 显示历史疗程列表（时间线）

自然语言输入与 AI 解析（DeepSeek，必须二次确认）：
- 用药管理页提供输入框，用户可输入自然语言描述
- 点击“解析/提交”后：
  - 调用 DeepSeek，将 rawText + 当前药品库名称列表 + 当前 active 疗程摘要 一起发送
  - DeepSeek 输出结构化 JSON（必须校验）：
    - mentioned_meds[]：识别到的药名（含是否在库中）
    - actions[]：建议动作列表
      - actionType: add_med / start_course / pause_course / end_course / update_course / noop
      - medName
      - courseFields: startDate? endDate? status? frequencyText? doseText? timeHints?
    - questions[]：最多 2 个澄清问题（优先选择题）
- App 展示“变更草稿确认面板”：
  - 新药默认生成 add_med 草稿，必须用户确认
  - 所有开始/暂停/停药操作必须可手动调整关键字段
- 用户确认后执行数据库变更，并保存 MedEvent
- AI 解析失败或 JSON 校验失败：
  - 明确提示失败原因
  - 不允许生成假数据

拍照 OCR 与摘要提取（可分阶段）：
- 药品详情页支持拍照/选图
- OCR 转文本（优先离线）
- 将文本发送 DeepSeek 抽取摘要：
  - name_candidates[]
  - dosage_summary
  - cautions_summary
  - adverse_summary
- 用户确认/编辑后写入 Med.infoSummary
- 明确声明：仅为说明书信息整理，不构成医疗建议

与日报联动：
- 日报中的“用药是否按时”保持不变
- AI 输入额外包含：
  - activeMedsSummary（当前正在吃的药）
  - todayMedChanges（当天用药事件摘要）
  - adherenceHint（是否按时）

====================
七、里程碑（新增：用药管理）
====================

M7：用药管理（无 OCR）
- Room：Med / MedCourse / MedEvent
- 用药管理页 + 药品详情页
- 结构化疗程管理（开始 / 暂停 / 结束）
- ./gradlew build 通过

M8：用药自然语言解析（DeepSeek）
- rawText 输入
- DeepSeek 返回结构化动作草稿
- UI 二次确认后落库
- JSON 校验与失败处理
- ./gradlew build 通过

M9：拍照 OCR + 摘要提取
- 拍照/选图 -> OCR -> DeepSeek 摘要
- 写入 Med.infoSummary
- ./gradlew build 通过

