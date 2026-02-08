# 发布新版本指南

## 快速发布流程

### 1. 准备发布
```bash
# 确保代码已提交
git status

# 拉取最新代码
git pull origin main
```

### 2. 更新版本号
编辑 `app/build.gradle.kts`：
```kotlin
android {
    defaultConfig {
        versionCode = 2  // 递增版本号
        versionName = "1.0.1"  // 更新版本名称
    }
}
```

### 3. 提交版本更新
```bash
git add app/build.gradle.kts
git commit -m "chore: bump version to 1.0.1"
git push origin main
```

### 4. 创建并推送Tag
```bash
# 创建带注释的tag
git tag -a v1.0.1 -m "Release v1.0.1

新增功能：
- 周度AI健康洞察自动生成
- 改进通知系统可靠性

修复问题：
- 修复周洞察日期计算错误
- 优化AI提示词质量
"

# 推送tag到远端（这会触发GitHub Actions自动构建）
git push origin v1.0.1
```

### 5. 等待自动构建
- 访问 [GitHub Actions](https://github.com/yourusername/HEl/actions)
- 查看构建状态（通常需要5-10分钟）
- 构建成功后会自动创建Release并上传APK

### 6. 检查Release
- 访问 [Releases页面](https://github.com/yourusername/HEl/releases)
- 确认APK文件已上传
- 必要时编辑Release说明

## 手动触发构建

如果需要手动触发构建（不发布tag）：

1. 访问 [Actions页面](https://github.com/yourusername/HEl/actions/workflows/build-release.yml)
2. 点击 "Run workflow"
3. 选择分支并运行
4. 构建的APK会作为Artifact保存（90天）

## 配置Release签名（可选）

### 生成Release密钥
```bash
keytool -genkey -v -keystore release.keystore -alias heldairy -keyalg RSA -keysize 2048 -validity 10000
```

### 在GitHub中配置密钥
1. 将keystore文件转为base64：
```bash
base64 release.keystore > release.keystore.base64
```

2. 在GitHub仓库设置中添加Secrets：
   - `KEYSTORE_FILE`: keystore.base64的内容
   - `KEYSTORE_PASSWORD`: keystore密码
   - `KEY_ALIAS`: 密钥别名（如 heldairy）
   - `KEY_PASSWORD`: 密钥密码

3. 更新 `app/build.gradle.kts`：
```kotlin
android {
    signingConfigs {
        create("release") {
            storeFile = file("../release.keystore")
            storePassword = System.getenv("KEYSTORE_PASSWORD")
            keyAlias = System.getenv("KEY_ALIAS")
            keyPassword = System.getenv("KEY_PASSWORD")
        }
    }
    
    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = true
            proguardFiles(/* ... */)
        }
    }
}
```

4. 更新 GitHub Actions workflow（已在 `.github/workflows/build-release.yml` 中）

## 版本号规范

采用语义化版本 (Semantic Versioning)：

- **MAJOR.MINOR.PATCH** (如 1.2.3)
  - MAJOR: 重大更新，可能包含破坏性变更
  - MINOR: 新增功能，向后兼容
  - PATCH: Bug修复，向后兼容

示例：
- `v1.0.0` - 首个正式版本
- `v1.0.1` - Bug修复
- `v1.1.0` - 新增功能
- `v2.0.0` - 重大更新

## 常见问题

### Q: 如何删除错误的tag？
```bash
# 删除本地tag
git tag -d v1.0.1

# 删除远端tag
git push --delete origin v1.0.1
```

### Q: 如何修改已发布的Release说明？
访问 [Releases页面](https://github.com/yourusername/HEl/releases)，点击对应版本的编辑按钮。

### Q: 构建失败怎么办？
1. 查看 [Actions页面](https://github.com/yourusername/HEl/actions) 的错误日志
2. 修复问题后重新提交
3. 删除错误的tag并重新创建

### Q: 如何发布Beta版本？
```bash
git tag -a v1.1.0-beta.1 -m "Beta release"
git push origin v1.1.0-beta.1
```
Beta版本会被标记为"Pre-release"。

## Checklist

发布前检查清单：

- [ ] 本地编译通过：`./gradlew build`
- [ ] 所有测试通过
- [ ] 更新了版本号（versionCode + versionName）
- [ ] 更新了CHANGELOG（如果有）
- [ ] 提交了所有代码变更
- [ ] 创建了正确的git tag
- [ ] 推送了tag到远端
- [ ] GitHub Actions构建成功
- [ ] 下载并测试了生成的APK
- [ ] 更新了Release说明

## 自动化改进建议

未来可以添加：
1. 自动从commit生成CHANGELOG
2. 自动递增版本号
3. 自动化测试流程
4. 发布到Google Play Store
5. 崩溃报告集成（如Firebase Crashlytics）
