package com.healthoracle.core.dashboard

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

// ─────────────────────────────────────────────────────────────────────────────
// DATA MODELS
// ─────────────────────────────────────────────────────────────────────────────

data class DailyHealthData(
    val steps: Int        = 0,      // measured steps
    val stepGoal: Int     = 10_000,
    val waterGlasses: Int = 0,      // glasses consumed
    val waterGoal: Int    = 8,
    val sleepHours: Float = 0f,     // hours slept last night
    val sleepGoal: Float  = 8f,
    val moodScore: Int    = 0,      // 1–5 (1=rough, 5=amazing)
    val date: String      = LocalDate.now()
        .format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
)

data class HealthScore(
    val total: Int,         // 0–100
    val stepsScore: Int,    // 0–25
    val waterScore: Int,    // 0–25
    val sleepScore: Int,    // 0–25
    val moodScore: Int,     // 0–25
    val label: String,      // "Excellent" / "Good" / "Fair" / "Needs Attention"
    val delta: Int          // change vs yesterday (positive = improved)
)

data class SmartInsight(
    val emoji: String,
    val message: String,
    val highlight: String,  // part of message to highlight in accent color
    val accentColorKey: InsightColor,
    val priority: Int       // lower = shown first
)

enum class InsightColor { BLUE, GREEN, AMBER, ROSE }

// ─────────────────────────────────────────────────────────────────────────────
// SCORE ENGINE
// ─────────────────────────────────────────────────────────────────────────────

object HealthScoreEngine {

    /**
     * Calculate a 0-100 composite health score.
     * Each pillar contributes max 25 points.
     */
    fun calculate(today: DailyHealthData, yesterday: DailyHealthData? = null): HealthScore {
        val stepsScore = calculateStepsScore(today.steps, today.stepGoal)
        val waterScore = calculateWaterScore(today.waterGlasses, today.waterGoal)
        val sleepScore = calculateSleepScore(today.sleepHours, today.sleepGoal)
        val moodScore  = calculateMoodScore(today.moodScore)

        val total = stepsScore + waterScore + sleepScore + moodScore

        val yesterdayTotal = yesterday?.let {
            calculateStepsScore(it.steps, it.stepGoal) +
            calculateWaterScore(it.waterGlasses, it.waterGoal) +
            calculateSleepScore(it.sleepHours, it.sleepGoal) +
            calculateMoodScore(it.moodScore)
        } ?: total

        val label = when {
            total >= 85 -> "Excellent"
            total >= 65 -> "Good"
            total >= 45 -> "Fair"
            else        -> "Needs Attention"
        }

        return HealthScore(
            total      = total,
            stepsScore = stepsScore,
            waterScore = waterScore,
            sleepScore = sleepScore,
            moodScore  = moodScore,
            label      = label,
            delta      = total - yesterdayTotal
        )
    }

    private fun calculateStepsScore(steps: Int, goal: Int): Int {
        val ratio = steps.toFloat() / goal.toFloat()
        return (ratio * 25f).coerceAtMost(25f).roundToInt()
    }

    private fun calculateWaterScore(glasses: Int, goal: Int): Int {
        val ratio = glasses.toFloat() / goal.toFloat()
        return (ratio * 25f).coerceAtMost(25f).roundToInt()
    }

    private fun calculateSleepScore(hours: Float, goal: Float): Int {
        return when {
            hours <= 0f         -> 0
            hours < goal - 2f   -> ((hours / goal) * 25f).roundToInt()
            hours in (goal - 2f)..(goal + 1f) -> 25   // optimal range
            hours > goal + 1f   -> 20                  // slight over-sleep penalty
            else                -> 0
        }
    }

    private fun calculateMoodScore(mood: Int): Int {
        return ((mood.coerceIn(0, 5).toFloat() / 5f) * 25f).roundToInt()
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// INSIGHT GENERATOR
// ─────────────────────────────────────────────────────────────────────────────

object InsightGenerator {

    /**
     * Produces a prioritised list of smart insights.
     * No ML required — pure rule-based logic using health data.
     */
    fun generate(today: DailyHealthData, yesterday: DailyHealthData? = null): List<SmartInsight> {
        val insights = mutableListOf<SmartInsight>()

        // ── Steps insights ────────────────────────────────────────────────────
        val stepsRemaining = today.stepGoal - today.steps
        when {
            today.steps == 0 -> insights.add(
                SmartInsight("🚶", "Start walking to earn your steps for today", "Start walking",
                    InsightColor.BLUE, 1)
            )
            stepsRemaining > 0 -> {
                val prevSteps = yesterday?.steps ?: today.steps
                val direction = if (today.steps < prevSteps) "less" else "more"
                insights.add(
                    SmartInsight(
                        "🚶",
                        "You walked $direction than yesterday — ${stepsRemaining.formatK()} steps to goal",
                        "${stepsRemaining.formatK()} steps to goal",
                        InsightColor.BLUE,
                        if (today.steps < prevSteps) 1 else 3
                    )
                )
            }
            else -> insights.add(
                SmartInsight("🏆", "Daily step goal achieved! Keep it up", "goal achieved",
                    InsightColor.GREEN, 5)
            )
        }

        // ── Water insights ────────────────────────────────────────────────────
        val waterRemaining = today.waterGoal - today.waterGlasses
        when {
            waterRemaining > 0 -> insights.add(
                SmartInsight(
                    "💧",
                    "Drink $waterRemaining more ${if (waterRemaining == 1) "glass" else "glasses"} of water today",
                    "$waterRemaining more ${if (waterRemaining == 1) "glass" else "glasses"}",
                    InsightColor.AMBER,
                    if (waterRemaining >= today.waterGoal / 2) 2 else 4
                )
            )
            else -> insights.add(
                SmartInsight("💧", "Hydration goal reached! Great job staying hydrated", "goal reached",
                    InsightColor.GREEN, 6)
            )
        }

        // ── Sleep insights ────────────────────────────────────────────────────
        val prevSleep = yesterday?.sleepHours ?: 0f
        when {
            today.sleepHours <= 0f -> insights.add(
                SmartInsight("🌙", "Log your sleep tonight to track your rest patterns",
                    "Log your sleep", InsightColor.AMBER, 3)
            )
            today.sleepHours < 6f -> insights.add(
                SmartInsight("🌙",
                    "Only ${today.sleepHours.format1dp()}h sleep — aim for ${today.sleepGoal.toInt()}h tonight",
                    "${today.sleepHours.format1dp()}h sleep",
                    InsightColor.ROSE, 1)
            )
            prevSleep > 0f && today.sleepHours > prevSleep + 0.5f -> insights.add(
                SmartInsight("🌙", "Sleep improved by ${(today.sleepHours - prevSleep).format1dp()}h this week",
                    "Sleep improved", InsightColor.GREEN, 4)
            )
            today.sleepHours >= today.sleepGoal -> insights.add(
                SmartInsight("🌙", "Perfect sleep last night — you're well-rested!",
                    "Perfect sleep", InsightColor.GREEN, 6)
            )
        }

        // ── Mood insights ─────────────────────────────────────────────────────
        when (today.moodScore) {
            0    -> insights.add(SmartInsight("😊", "Log your mood today to track your emotional wellbeing",
                "Log your mood", InsightColor.AMBER, 3))
            1, 2 -> insights.add(SmartInsight("❤️",
                "Rough day? A short walk can boost your mood significantly",
                "short walk can boost", InsightColor.ROSE, 2))
            4, 5 -> insights.add(SmartInsight("✨",
                "Great mood today! You're doing amazing", "doing amazing", InsightColor.GREEN, 5))
        }

        // ── Health check reminder ─────────────────────────────────────────────
        insights.add(
            SmartInsight("🔬", "Run your monthly diabetes risk check to stay informed",
                "monthly diabetes risk check", InsightColor.ROSE, 7)
        )

        return insights.sortedBy { it.priority }.take(5)
    }

    private fun Int.formatK(): String =
        if (this >= 1000) "${this / 1000}.${(this % 1000) / 100}k" else "$this"

    private fun Float.format1dp(): String = "%.1f".format(this)
}
