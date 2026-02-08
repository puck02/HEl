# GitHub Actions 自动发布使用指南

## 📦 已配置完成

以下文件已创建：
- `.github/workflows/build-release.yml` - GitHub Actions工作流配置
- `RELEASE.md` - 详细的发布流程文档
- `CHANGELOG.md` - 版本变更记录模板
- `README.md` - 已更新下载链接

## 🚀 快速开始

### 第一次发布

1. **提交所有当前代码**
```bash
# 添加新文件
git add .github/workflows/build-release.yml
git add RELEASE.md CHANGELOG.md

# 提交最新修改
git add -A
git commit -m "feat: add GitHub Actions auto-build and release workflow

- Auto-build APK on tag push
- Create GitHub Release with APK artifacts
- Add release documentation
- Update README with download links
"

# 推送到主分支
git push origin main
```

2. **创建第一个发布版本**
```bash
# 创建tag（这会触发自动构建）
git tag -a v1.0.0 -m "Release v1.0.0 - 首个正式版本

✨ 新功能
- 每日健康问诊对话流程
- DeepSeek AI智能建议
- 周度健康洞察自动生成
- 用药管理和提醒
- 医生报告PDF导出
- 数据导出/导入功能

🐛 修复
- 修复周洞察日期计算错误
- 修复通知系统可靠性
- 优化AI返回JSON验证

📱 系统要求
- Android 10+ (API 29+)
- 需要配置DeepSeek API Key
"

# 推送tag到GitHub
git push origin v1.0.0
```

3. **查看构建进度**
   - 访问: https://github.com/puck02/HEl/actions
   - 等待5-10分钟构建完成
   - 绿色✅表示成功，红色❌表示失败

4. **检查Release**
   - 访问: https://github.com/puck02/HEl/releases
   - 确认APK已上传
   - 测试下载并安装

## 📝 后续版本发布

### 标准流程

每次发布新版本时：

```bash
# 1. 更新版本号（编辑 app/build.gradle.kts）
# versionCode = 2
# versionName = "1.0.1"

# 2. 更新 CHANGELOG.md
# 记录本次更新内容

# 3. 提交代码
git add app/build.gradle.kts CHANGELOG.md
git commit -m "chore: bump version to 1.0.1"
git push origin main

# 4. 创建并推送tag
git tag -a v1.0.1 -m "Release v1.0.1

修复问题：
- 修复AI洞察格式验证错误
- 改进错误提示信息
"
git push origin v1.0.1
```

### 快速修复版本

```bash
# Bug修复后直接打tag
git tag -a v1.0.2 -m "Hotfix v1.0.2: 修复严重bug"
git push origin v1.0.2
```

## 🔧 GitHub仓库设置

### 必需配置

1. **启用GitHub Actions**
   - 进入仓库Settings → Actions → General
   - 选择"Allow all actions and reusable workflows"
   - 保存更改

2. **配置Workflow权限**
   - Settings → Actions → General → Workflow permissions
   - 选择"Read and write permissions"
   - 勾选"Allow GitHub Actions to create and approve pull requests"
   - 保存

3. **更新README中的链接**
   - 将`puck02`替换为你的GitHub用户名
   - 或使用完整仓库路径

### 可选配置（Release签名）

如果需要发布正式签名的APK：

1. **生成签名密钥**
```bash
keytool -genkey -v -keystore release.keystore \
  -alias heldairy -keyalg RSA -keysize 2048 -validity 10000
```

2. **转换为Base64**
```bash
base64 release.keystore > release.keystore.base64
```

3. **在GitHub添加Secrets**
   - Settings → Secrets and variables → Actions
   - 添加以下secrets:
     - `KEYSTORE_FILE`: keystore.base64的内容
     - `KEYSTORE_PASSWORD`: keystore密码
     - `KEY_ALIAS`: heldairy
     - `KEY_PASSWORD`: 密钥密码

4. **更新workflow**（可选，当前已配置使用debug签名）

## 📋 版本号规范

采用**语义化版本**：`MAJOR.MINOR.PATCH`

- **MAJOR** (1.x.x): 重大更新，可能不向后兼容
  - 例：v2.0.0 - 重构整体架构
  
- **MINOR** (x.1.x): 新功能，向后兼容
  - 例：v1.1.0 - 添加数据同步功能
  
- **PATCH** (x.x.1): Bug修复，向后兼容
  - 例：v1.0.1 - 修复崩溃问题

### 特殊版本

- **Beta测试**: v1.1.0-beta.1
- **Release Candidate**: v1.1.0-rc.1
- **Nightly构建**: 手动触发workflow，不打tag

## 🎯 自动构建说明

### 触发条件

GitHub Actions会在以下情况自动构建：

1. **推送tag** (推荐)
   ```bash
   git tag -a v1.0.0 -m "Release notes"
   git push origin v1.0.0
   ```
   - ✅ 自动创建GitHub Release
   - ✅ 自动上传APK
   - ✅ 添加release说明

2. **手动触发**
   - 访问Actions页面
   - 选择"Build and Release APK"工作流
   - 点击"Run workflow"
   - ⚠️ 不会创建Release，只生成Artifact

### 构建产物

每次构建会生成：

1. **Debug APK** (HElDairy-vX.X.X-debug.apk)
   - 包含调试信息
   - APK较大（约15-20MB）
   - 适合测试

2. **Release APK** (HElDairy-vX.X.X-release.apk)
   - 已优化压缩
   - APK较小（约8-12MB）
   - 生产环境使用
   - ⚠️ 需要配置签名，否则为unsigned版本

### 存储位置

- **GitHub Releases**: 永久存储，可公开下载
- **Actions Artifacts**: 临时存储90天，需登录GitHub下载

## ❓ 常见问题

### Q: 构建失败怎么办？

1. 查看Actions日志找出错误原因
2. 常见问题：
   - Gradle版本不兼容：检查`gradle-wrapper.properties`
   - 编译错误：本地先运行`./gradlew build`测试
   - 网络问题：重新运行workflow

3. 修复后重新推送tag：
```bash
git tag -d v1.0.0  # 删除本地tag
git push --delete origin v1.0.0  # 删除远端tag
# 修复代码后重新创建tag
git tag -a v1.0.0 -m "..."
git push origin v1.0.0
```

### Q: 如何测试workflow不发布Release？

使用手动触发功能：
1. 访问Actions页面
2. 点击"Run workflow"
3. 选择分支运行
4. 查看Artifacts下载APK测试

### Q: 能否自动发布到Google Play？

可以，需要：
1. Google Play服务账号JSON密钥
2. 添加为GitHub Secret
3. 修改workflow添加发布步骤
4. 参考：[r0adkll/upload-google-play](https://github.com/r0adkll/upload-google-play)

### Q: 如何回滚版本？

GitHub Release支持：
1. 删除错误的Release和tag
2. 重新创建正确版本的tag
3. 或者在Releases页面编辑说明

### Q: 多久构建一次？

建议：
- **Patch版本**：Bug修复后立即发布
- **Minor版本**：积累一些功能后发布（1-2周）
- **Major版本**：重大更新时发布（数月）

## 📊 下一步优化

可以添加的功能：

1. **自动化测试**
   - 运行单元测试
   - UI测试
   - 集成测试

2. **代码质量检查**
   - Lint检查
   - Ktlint格式化
   - Detekt静态分析

3. **自动生成CHANGELOG**
   - 从commit messages生成
   - 使用conventional commits

4. **多渠道打包**
   - Google Play版本
   - 华为应用市场版本
   - 其他渠道

5. **崩溃报告**
   - Firebase Crashlytics
   - Sentry集成

## 📚 相关文档

- [RELEASE.md](RELEASE.md) - 详细发布流程
- [CHANGELOG.md](CHANGELOG.md) - 版本变更记录
- [requirements.md](doc/requirements.md) - 项目需求文档
- [GitHub Actions文档](https://docs.github.com/en/actions)

---

**重要提示**: 
1. 记得将README.md中的`puck02`替换为你的GitHub用户名
2. 首次推送tag前确保代码已经过完整测试
3. Release版本建议配置签名密钥以提供正式签名的APK
