package com.trainingapp.data.model

import java.time.LocalDate

/**
 * Represents a community fitness challenge the user can join.
 *
 * @param id              Unique identifier.
 * @param title           Short challenge name.
 * @param description     Detailed description of what the challenge requires.
 * @param relatedCategory The [WorkoutCategory] that counts toward this challenge.
 *                        Null means any workout type is accepted.
 * @param targetCount     How many completed workouts are needed to finish the challenge.
 * @param startDate       First day of the challenge window (inclusive).
 * @param endDate         Last day of the challenge window (inclusive).
 * @param participants    Total number of enrolled participants.
 * @param isJoined        Whether the current user has joined this challenge.
 * @param progressPct     Current progress toward completing the challenge [0..100].
 *                        Only counts workouts completed within [startDate]..[endDate].
 */
data class Challenge(
    val id: Int,
    val title: String,
    val description: String,
    val relatedCategory: WorkoutCategory?,
    val targetCount: Int,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val participants: Int,
    val isJoined: Boolean = false,
    val progressPct: Int = 0
)

/** Pre-built sample challenges shown on the Challenges screen. */
object SampleChallenges {
    /**
     * Date ranges are anchored to [LocalDate.now] so the sample data always
     * looks "live" regardless of when the app is first launched.
     */
    val all: List<Challenge> = run {
        val today = LocalDate.now()
        listOf(
            Challenge(
                id = 1,
                title = "7 днів кардіо",
                description = "Виконуй кардіо-тренування щодня протягом тижня",
                relatedCategory = WorkoutCategory.CARDIO,
                targetCount = 7,
                startDate = today.minusDays(4),
                endDate = today.plusDays(3),
                participants = 142,
                isJoined = true
            ),
            Challenge(
                id = 2,
                title = "100 віджимань",
                description = "Виконай 100 віджимань протягом одного тижня",
                relatedCategory = WorkoutCategory.STRENGTH,
                targetCount = 5,
                startDate = today.minusDays(2),
                endDate = today.plusDays(5),
                participants = 89
            ),
            Challenge(
                id = 3,
                title = "Ранковий старт",
                description = "Тренуйся тричі на тиждень — будь-яка категорія",
                relatedCategory = null,
                targetCount = 3,
                startDate = today.minusDays(1),
                endDate = today.plusDays(6),
                participants = 213
            ),
            Challenge(
                id = 4,
                title = "Силовий квітень",
                description = "Завершуй мінімум одне силове тренування на тиждень",
                relatedCategory = WorkoutCategory.STRENGTH,
                targetCount = 1,
                startDate = today.minusDays(12),
                endDate = today.plusDays(14),
                participants = 67,
                isJoined = true
            )
        )
    }
}
