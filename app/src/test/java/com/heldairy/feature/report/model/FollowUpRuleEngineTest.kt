package com.heldairy.feature.report.model

import com.heldairy.core.data.TrendFlag
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FollowUpRuleEngineTest {
    @Test
    fun triggersHeadacheFollowUpsWhenSevere() {
        val answers = mapOf(
            "headache_intensity" to DailyAnswerPayload.Slider(7)
        )

        val followUps = FollowUpRuleEngine.evaluate(
            answers = answers,
            trends = emptyMap()
        )

        assertTrue(followUps.any { it.id == "fu_headache_nature" })
    }

    @Test
    fun triggersOnRisingTrendEvenIfMild() {
        val answers = mapOf(
            "headache_intensity" to DailyAnswerPayload.Slider(4)
        )

        val followUps = FollowUpRuleEngine.evaluate(
            answers = answers,
            trends = mapOf("headache_intensity" to TrendFlag.rising)
        )

        assertTrue(followUps.any { it.id == "fu_headache_pattern" })
    }

    @Test
    fun doesNotTriggerWhenCalmAndStable() {
        val answers = mapOf(
            "headache_intensity" to DailyAnswerPayload.Slider(2)
        )

        val followUps = FollowUpRuleEngine.evaluate(
            answers = answers,
            trends = emptyMap()
        )

        assertFalse(followUps.any { it.id.startsWith("fu_headache") })
    }
}
