# 深入排查：AI 建议“格式不正确/空内容”问题

## 结论概览
- 根因：DeepSeek 返回的 `observations`/`actions` 等字段有时是对象（含 suggestion/text 等）或混合结构，早期解析只接受字符串数组，导致判定为空并抛“格式不正确”。
- 修复：改为宽容解析（对象提取 suggestion/text/message/content/value/detail），并在解析/校验失败时自动兜底；添加一次重试，仍为空则落基础建议并标记 fallback。
- 状态：当前调用已成功，UI 会在兜底时显示“AI 建议暂时不可用，以下为基础建议”。

## 现网行为与用户体验
- 首次 DeepSeek 响应解析失败或字段为空：重试 1 次；若仍空，保存兜底建议并标记来源为 fallback。
- UI 提示：展示生成时间，若为 fallback，显示醒目的“AI 建议暂时不可用，以下为基础建议”。
- 日志：仅记录长度、SHA、键名/数组大小（无明文），便于后续排查。

## 关键改动
1) **宽容解析**：
   - 文件：[app/src/main/java/com/heldairy/core/network/DeepSeekClient.kt](../app/src/main/java/com/heldairy/core/network/DeepSeekClient.kt)
   - 逻辑：
     - 提取 JSON 后，逐字段尝试：字符串数组 / 对象数组 / 单对象；对象优先取 `suggestion/text/message/content/value/detail`。
     - 解析失败时记录结构摘要（键名、数组大小），不记录明文。

2) **重试与兜底**：
   - 文件：[app/src/main/java/com/heldairy/core/data/DailyAdviceCoordinator.kt](../app/src/main/java/com/heldairy/core/data/DailyAdviceCoordinator.kt)
   - 逻辑：
     - fetch 失败或内容空 → 重试 1 次；仍空 → 生成安全兜底建议（标记 `source=fallback`）并存储。
     - 解析异常也走兜底，避免再现“格式不正确”提示。

3) **来源标记与 UI 提示**：
   - 数据：[app/src/main/java/com/heldairy/core/data/DailyAdviceModels.kt](../app/src/main/java/com/heldairy/core/data/DailyAdviceModels.kt)
   - UI：[app/src/main/java/com/heldairy/feature/report/ui/DailyReportScreen.kt](../app/src/main/java/com/heldairy/feature/report/ui/DailyReportScreen.kt)
   - 逻辑：AdvicePayload 增加 `source`，ViewModel/界面根据 `fallback` 显示提示文案。

## 触发原因细节（为何之前空）
- DeepSeek 在部分响应中返回对象数组（示例中 `actions` 是对象），旧解析只接受字符串 → 视为空，触发校验失败。
- 多次空判定导致持续弹“格式不正确”，未落兜底。

## 建议与后续
- 若未来仍遇到解析异常，先抓 `adb logcat -d | grep -E "DeepSeekClient|DailyAdviceCoord"` 查看键名/数组大小，再按键名扩展对象提取规则。
- 如需更强健：可加固定 schema 校验（必含 observations/actions 非空）并在 prompt 中提供示例 JSON。当前已满足线上稳定性。
