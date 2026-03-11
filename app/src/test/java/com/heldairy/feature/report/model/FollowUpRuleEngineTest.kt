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

    @Test
    fun triggersSleepFollowUpsWhenFocusPriorityContainsSleep() {
        val answers = mapOf(
            "sleep_duration" to DailyAnswerPayload.Choice("6_7"),
            "focus_priority" to DailyAnswerPayload.MultiChoice(setOf("sleep"))
        )

        val followUps = FollowUpRuleEngine.evaluate(
            answers = answers,
            trends = emptyMap()
        )

        assertTrue(followUps.any { it.id == "fu_sleep_issue" })
    }

    @Test
    fun triggersSleepFollowUpsWhenSleepIsFragmented() {
        val answers = mapOf(
            "sleep_duration" to DailyAnswerPayload.Choice("fragmented")
        )

        val followUps = FollowUpRuleEngine.evaluate(
            answers = answers,
            trends = emptyMap()
        )

        assertTrue(followUps.any { it.id == "fu_sleep_hygiene" })
    }

    @Test
    fun triggersFatigueFollowUpsWhenPrioritySelected() {
        val answers = mapOf(
            "mood_irritability" to DailyAnswerPayload.Slider(3),
            "focus_priority" to DailyAnswerPayload.MultiChoice(setOf("fatigue"))
        )

        val followUps = FollowUpRuleEngine.evaluate(
            answers = answers,
            trends = emptyMap()
        )

        assertTrue(followUps.any { it.id == "fu_fatigue_timing" })
    }
}
