# BugFix: Alpha值超出范围导致崩溃 - 2026-02-03

## Issue
应用启动即闪退，logcat显示：
```
java.lang.IllegalArgumentException: red = 1.0, green = 0.48235294, blue = 0.48235294, alpha = 1.0999999 outside the range for sRGB IEC61966-2.1
```

## Root Cause
`MetricSpark` composable中alpha值计算错误：
```kotlin
// 原有代码
targetValue = 0.5f + (index * 0.12f)
```

当weeklyData有7个元素时：
- index=6: `0.5 + (6 * 0.12) = 1.22` ❌ **超出[0, 1]范围**

## Solution
使用 `.coerceIn()` 限制alpha在合法范围内：
```kotlin
// 修复后
targetValue = (0.5f + (index * 0.08f)).coerceIn(0.5f, 1.0f)
```

同时降低alpha增量（0.12 → 0.08），使7根柱子的透明度渐变更平滑。

## Verification
```bash
$ ./gradlew installDebug
BUILD SUCCESSFUL in 34s

$ adb logcat -c && adb shell am start -n com.heldairy/.MainActivity
# No crash, app running normally
```

## Files Changed
- `MainActivity.kt` line 856: 修改alpha计算逻辑

## Lesson Learned
当使用动态数量的UI元素时（如根据数据动态生成柱状图），必须考虑：
1. 边界值检查（最大/最小数量）
2. 计算公式的极限情况
3. Color alpha必须在[0.0, 1.0]范围内

建议在类似场景添加 `require(alpha in 0f..1f)` 断言。
