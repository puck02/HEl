# 🔐 配置GitHub Secrets完整指南

## ⚠️ 重要提醒
旧keystore密码不正确导致CI签名失败。已生成新keystore，请按以下步骤配置。

---

## 📋 准备工作

### 1. 获取keystore的Base64编码

文件位置：`/Users/ponepuck/Library/CloudStorage/OneDrive-Personal/workSpace/HEl/app/keystore.jks.base64`

打开终端，复制文件内容：

```bash
cat /Users/ponepuck/Library/CloudStorage/OneDrive-Personal/workSpace/HEl/app/keystore.jks.base64 | pbcopy
```

这会将base64内容复制到剪贴板（3665字节）。

---

## 🔑 配置GitHub Secrets

### 步骤1：打开Secrets设置页面

访问：https://github.com/puck02/HEl/settings/secrets/actions

### 步骤2：添加4个Secrets

依次点击"New repository secret"添加：

#### Secret #1: KEYSTORE_FILE
- **Name**: `KEYSTORE_FILE`
- **Value**: 粘贴剪贴板中的base64内容（刚才复制的）
- 点击"Add secret"

#### Secret #2: KEYSTORE_PASSWORD
- **Name**: `KEYSTORE_PASSWORD`
- **Value**: `heldairy2024`
- 点击"Add secret"

#### Secret #3: KEY_ALIAS
- **Name**: `KEY_ALIAS`
- **Value**: `heldairy`
- 点击"Add secret"

#### Secret #4: KEY_PASSWORD
- **Name**: `KEY_PASSWORD`
- **Value**: `heldairy2024`
- 点击"Add secret"

---

## ✅ 验证配置

### 检查清单

配置完成后，确认以下4个Secrets都已添加：

```
✓ KEYSTORE_FILE (Updated X seconds/minutes ago)
✓ KEYSTORE_PASSWORD (Updated X seconds/minutes ago)
✓ KEY_ALIAS (Updated X seconds/minutes ago)
✓ KEY_PASSWORD (Updated X seconds/minutes ago)
```

---

## 🚀 触发签名构建

### 步骤1：删除本地base64文件（安全考虑）

```bash
rm /Users/ponepuck/Library/CloudStorage/OneDrive-Personal/workSpace/HEl/app/keystore.jks.base64
```

### 步骤2：重新创建tag触发CI

```bash
cd /Users/ponepuck/Library/CloudStorage/OneDrive-Personal/workSpace/HEl

# 创建并推送tag
git tag v1.0.0-kitty
git push origin v1.0.0-kitty
```

### 步骤3：监控构建状态

打开Actions页面：https://github.com/puck02/HEl/actions

等待构建完成（约3-5分钟）。

---

## 📥 下载并测试

### 下载Release APK

1. 打开：https://github.com/puck02/HEl/releases/tag/v1.0.0-kitty
2. 下载：`heldairy-v1.0.0-kitty-release.apk`

### 在Android设备上测试

传输APK到手机并安装：

```bash
adb install -r heldairy-v1.0.0-kitty-release.apk
```

如果能成功安装，说明签名配置正确！✅

---

## 🔍 故障排查

### 问题1：CI构建失败 "keystore password was incorrect"

**原因**: GitHub Secrets配置错误

**解决**:
1. 检查 `KEYSTORE_PASSWORD` 是否为 `heldairy2024`（无多余空格）
2. 检查 `KEY_PASSWORD` 是否为 `heldairy2024`
3. 检查 `KEY_ALIAS` 是否为 `heldairy`（无大写）

### 问题2：APK安装失败 "Package appears to be corrupt"

**原因**: KEYSTORE_FILE内容不完整

**解决**:
1. 重新执行步骤1的`pbcopy`命令
2. 删除旧的 `KEYSTORE_FILE` Secret
3. 重新添加，确保完整粘贴（应该是一长串字符，约3665字节）

### 问题3：构建成功但APK签名验证失败

**原因**: 签名配置未生效

**解决**:
1. 检查 `app/build.gradle.kts` 中 `signingConfig = signingConfigs.getByName("release")`
2. 查看CI日志中是否有 `✅ Keystore decoded successfully`
3. 检查workflow文件中decode步骤是否执行

---

## 📝 新keystore信息

```
密钥别名: heldairy
密钥库密码: heldairy2024
密钥密码: heldairy2024
算法: RSA 2048位
有效期: 2026-02-08 至 2053-06-26 (27年)
证书DN: CN=HElDairy, OU=Android, O=HElDairy, L=Beijing, ST=Beijing, C=CN
SHA256: 7C:A5:E7:20:91:7D:2F:9E:5C:71:1C:FC:CF:74:4E:63:BE:28:C4:38:3C:4A:AC:4A:EB:1E:14:EF:B9:95:19:6C
```

---

## 🎉 完成确认

配置成功后，你将看到：

1. ✅ CI构建成功（绿色✓）
2. ✅ GitHub Release自动创建
3. ✅ 两个APK文件可下载（debug + release）
4. ✅ Release APK可在Android设备上安装

---

## 📞 需要帮助？

如果遇到问题，请检查：

1. GitHub Actions日志：https://github.com/puck02/HEl/actions
2. Secrets配置页面：https://github.com/puck02/HEl/settings/secrets/actions
3. 完整文档：[SIGNING_SETUP.md](SIGNING_SETUP.md)

---

**注意事项**:

- 🔒 keystore.jks已在.gitignore中，不会提交到git
- 🔒 base64文件用完即删，避免泄露
- 🔒 不要在任何公开渠道分享密码或base64内容
- 🔒 旧keystore已备份为keystore.jks.old（仅本地）
