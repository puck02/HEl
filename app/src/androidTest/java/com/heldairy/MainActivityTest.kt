package com.heldairy

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * 端到端 UI 测试基础设施
 * 
 * 提供跨屏幕的集成测试能力，验证完整用户流程。
 * 使用 createAndroidComposeRule 启动真实 Activity。
 */
@RunWith(AndroidJUnit4::class)
class MainActivityTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun appLaunches_showsBottomNavigation() {
        // 验证 4 个底部 Tab 可见
        composeTestRule.onNodeWithText("首页").assertIsDisplayed()
        composeTestRule.onNodeWithText("日报").assertIsDisplayed()
        composeTestRule.onNodeWithText("洞察").assertIsDisplayed()
        composeTestRule.onNodeWithText("用药").assertIsDisplayed()
    }

    @Test
    fun switchToMedicationTab_showsMedicationList() {
        // 点击用药 Tab
        composeTestRule.onNodeWithText("用药").performClick()

        // 验证用药列表屏幕加载（根据实际UI调整）
        composeTestRule.waitUntil(timeoutMillis = 3000) {
            composeTestRule
                .onAllNodesWithText("添加用药")
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
    }

    @Test
    fun navigateToDailyReport_fillsAndSubmits() {
        // 点击日报 Tab
        composeTestRule.onNodeWithText("日报").performClick()

        // TODO: 完整填写日报流程
        // 1. 选择问候语选项
        // 2. 填写基线指标
        // 3. 提交并验证成功
    }
}
