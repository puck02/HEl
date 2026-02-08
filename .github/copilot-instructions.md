# HElDairy Copilot Instructions

## 🚨 开发流程约束
- **禁止自动提交与发布**: 编译完成后，**严禁**自动执行 `git commit`、`git push` 或 `git tag` 等操作。必须先向用户说明修改内容并征得明确同意后，才可提交代码或触发 CI/CD。
- **禁止自动触发 Action**: 创建或推送 Git tag 会触发 GitHub Actions 自动构建，必须获得用户授权后才能执行。

## 项目核心
- **Project Context**: [requirements.md](../requirements.md) defines an Android 10+ "AI private health concierge" app built with Kotlin, Jetpack Compose, Material 3, Room, ViewModel + StateFlow, Retrofit/OkHttp, and kotlinx.serialization.
- **Target Experience**: The core flow is a single conversational daily report that adapts follow-up questions and delivers same-session lifestyle advice; no generic forms or medical diagnoses.
- **Dialogue Flow**: Implement Step 0 greetings (2 fixed multiple-choice questions), Step 1 baseline metrics (mix of categorical choices and 0-10 sliders), Step 2 adaptive follow-ups triggered by symptom severity/trends, and Step 3 AI-generated advice JSON.
- **Adaptive Logic**: Step 2 first uses a deterministic rule tree (per-symptom question sets) before invoking AI for up to 2 supplemental questions; guard against asking duplicates and keep prompts short.
- **AI Contracts**: DeepSeek Chat Completions must produce strictly validated JSON structures for supplemental questions (text + type + options) and for advice results (observations/actions/tomorrow_focus/red_flags). Reject malformed payloads and show a retry option rather than fabricating content.
- **Data Ownership**: Room is the single source for all answered questions, follow-ups, and cached AI advice; avoid remote sync besides the DeepSeek API.
- **Memory Windows**: AI calls only receive "today's full answers" plus locally computed 7-day summaries; never dump raw history or entire tables into prompts.
- **Summaries**: Maintain rolling stats (7-day, 30-day averages, trend flags, anomaly counts) locally to power adaptive triggers and to feed the AI; store alongside metadata for quick retrieval.
- **UI Guidance**: Compose screens should feel like a guided conversation; render questions sequentially, hide future steps until previous ones finish, and reuse Material Icons for state cues (no custom images per requirements).
- **Resilience**: If DeepSeek is offline, surface "AI 建议暂时不可用" and skip advice generation for that session; do not store placeholder advice.
- **API Key Handling**: Provide a settings screen for users to paste their DeepSeek key, store it locally (SharedPreferences or encrypted variant), and offer a toggle to disable AI features without removing saved data.
- **Networking Stack**: Use Retrofit with OkHttp interceptors for auth header injection and request logging; parse JSON via kotlinx.serialization to enforce schemas.
- **Rule Trees**: Encode symptom follow-up trees in deterministic data structures (sealed classes or JSON assets) so they can evolve without touching business logic; sample branches are in [requirements.md](../requirements.md).
- **Export/Import**: Implement Storage Access Framework flows for both JSON backups (schemaVersion + full records) and CSV doctor exports; importing JSON performs an overwrite restore after validating the schema.
- **Build Discipline**: Work in milestones M0–M6 exactly as defined in [requirements.md](../requirements.md); after finishing each milestone run `./gradlew build`, stop, and report verification steps before moving on.
- **Final Packaging**: Milestone M6 requires `./gradlew assembleDebug` plus a README describing installation, migration, and API-key configuration.
- **Environment Setup**: Follow [dev_logs/2026-01-13.md](../dev_logs/2026-01-13.md) to ensure Android Studio is installed, `ANDROID_HOME=/Users/ponepuck/Library/Android/sdk`, and platform-tools are on PATH.
- **Testing Expectations**: Favor Gradle unit tests for business logic (adaptive triggers, JSON validators) and Compose previews/manual testing for UI; always run `./gradlew build` before sharing progress.
- **No Cloud Storage**: Persist everything locally; avoid Firebase/Cloud backups unless future requirements change.
- **Tone & Copy**: User-facing text should adopt the "生活管家" voice—warm, calm, and non-diagnostic; never suggest medication changes.
- **Security**: Do not log health data or API responses in clear text; scrub logs in production builds.
- **Offline Behavior**: Core daily report must function without network (store responses, queue AI calls if disabled, but block advice until connectivity returns).
- **Icons & Assets**: Use Material Icons only; avoid external bitmaps to keep the APK small and offline-friendly.
- **Future-Proofing**: Structure modules so milestone M3+ dynamic question insertion is just StateFlow updates rather than rebuilding navigation stacks.
- **Deliverable Reminder**: Final handoff needs APK + documentation of export/import formats and AI setup steps per requirement M6.
