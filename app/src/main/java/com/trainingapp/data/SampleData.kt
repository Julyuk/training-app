package com.trainingapp.data

import com.trainingapp.data.model.*
import java.time.LocalDate

/**
 * Hard-coded sample data used to populate the UI without a database.
 * In a production app this would be replaced by a repository backed by Room or a remote API.
 */
object SampleData {

    private val allExercises = listOf(
        Exercise(
            id = 1,
            name = "Жим лежачи",
            muscleGroup = "Грудні м'язи",
            sets = 4,
            reps = 10,
            weightKg = 60f,
            isBodyweight = false,
            notes = "Тримати спину рівно, лопатки зведені"
        ),
        Exercise(
            id = 2,
            name = "Присідання зі штангою",
            muscleGroup = "Ноги",
            sets = 4,
            reps = 12,
            weightKg = 80f,
            isBodyweight = false,
            notes = "Коліна не виходять за носки, глибина — паралель"
        ),
        Exercise(
            id = 3,
            name = "Підтягування",
            muscleGroup = "Спина",
            sets = 3,
            reps = 8,
            weightKg = 0f,
            isBodyweight = true,
            notes = "Повна амплітуда, без розгойдування"
        ),
        Exercise(
            id = 4,
            name = "Планка",
            muscleGroup = "Прес",
            sets = 3,
            reps = 1,
            weightKg = 0f,
            isBodyweight = true,
            notes = "Тримати 60 секунд кожен підхід"
        ),
        Exercise(
            id = 5,
            name = "Станова тяга",
            muscleGroup = "Спина / Ноги",
            sets = 3,
            reps = 8,
            weightKg = 100f,
            isBodyweight = false,
            notes = "Спина рівна, погляд вперед"
        ),
        Exercise(
            id = 6,
            name = "Жим гантелей на похилій лаві",
            muscleGroup = "Грудні м'язи",
            sets = 3,
            reps = 12,
            weightKg = 22f,
            isBodyweight = false,
            notes = "Кут лави 30–45 градусів"
        ),
        Exercise(
            id = 7,
            name = "Бічні підйоми гантелей",
            muscleGroup = "Плечі",
            sets = 3,
            reps = 15,
            weightKg = 10f,
            isBodyweight = false,
            notes = "Без розгону, контрольований рух"
        ),
        Exercise(
            id = 8,
            name = "Берпі",
            muscleGroup = "Все тіло",
            sets = 4,
            reps = 15,
            weightKg = 0f,
            isBodyweight = true,
            notes = "Повний стрибок вгору, грудна клітка торкається підлоги"
        )
    )

    val workouts: List<Workout> = listOf(
        Workout(
            id = 1,
            title = "Грудні м'язи та трицепс",
            description = "Базове тренування на верхню частину тіла з акцентом на грудні м'язи.",
            durationMinutes = 60,
            caloriesBurned = 420,
            isCompleted = true,
            date = LocalDate.now().minusDays(1),
            category = WorkoutCategory.STRENGTH,
            exercises = listOf(allExercises[0], allExercises[5], allExercises[6])
        ),
        Workout(
            id = 2,
            title = "День ніг",
            description = "Комплексне тренування на м'язи нижньої частини тіла.",
            durationMinutes = 75,
            caloriesBurned = 550,
            isCompleted = true,
            date = LocalDate.now().minusDays(3),
            category = WorkoutCategory.STRENGTH,
            exercises = listOf(allExercises[1], allExercises[4])
        ),
        Workout(
            id = 3,
            title = "HIIT кардіо",
            description = "Інтервальне тренування для ефективного спалювання жиру.",
            durationMinutes = 30,
            caloriesBurned = 380,
            isCompleted = false,
            date = LocalDate.now(),
            category = WorkoutCategory.HIIT,
            exercises = listOf(allExercises[7], allExercises[3])
        ),
        Workout(
            id = 4,
            title = "Спина та біцепс",
            description = "Тянучі вправи для розвитку м'язів спини та рук.",
            durationMinutes = 55,
            caloriesBurned = 390,
            isCompleted = true,
            date = LocalDate.now().minusDays(5),
            category = WorkoutCategory.STRENGTH,
            exercises = listOf(allExercises[2], allExercises[4])
        ),
        Workout(
            id = 5,
            title = "Функціональне тренування",
            description = "Комплекс вправ із власною вагою для всього тіла.",
            durationMinutes = 45,
            caloriesBurned = 300,
            isCompleted = false,
            date = LocalDate.now().plusDays(1),
            category = WorkoutCategory.FLEXIBILITY,
            exercises = listOf(allExercises[2], allExercises[3], allExercises[7])
        )
    )

    val userProfile = UserProfile(
        name = "Юлія Українець",
        age = 22,
        weightKg = 58.5f,
        heightCm = 165,
        fitnessGoal = "Підтримання форми та розвиток сили",
        weeklyWorkoutTarget = 4,
        isPremium = false
    )

    fun findWorkoutById(id: Int): Workout? = workouts.find { it.id == id }

    fun findExerciseById(id: Int): Exercise? =
        workouts.flatMap { it.exercises }.distinctBy { it.id }.find { it.id == id }
}
