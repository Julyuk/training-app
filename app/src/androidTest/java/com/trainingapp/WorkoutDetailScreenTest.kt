package com.trainingapp

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.trainingapp.data.SampleData
import com.trainingapp.ui.screens.WorkoutDetailScreen
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented UI tests for [WorkoutDetailScreen].
 *
 * Verifies that the detail screen renders all data from the [Workout] model
 * object and that navigation affordances (back button) work correctly.
 */
@RunWith(AndroidJUnit4::class)
class WorkoutDetailScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    // Use the first sample workout which has a known set of exercises
    private val workout = SampleData.workouts.first()

    private fun setContent(
        onBack: () -> Unit = {},
        onEditClick: () -> Unit = {},
        onExerciseClick: (com.trainingapp.data.model.Exercise) -> Unit = {}
    ) {
        composeTestRule.setContent {
            WorkoutDetailScreen(
                workout = workout,
                onBack = onBack,
                onEditClick = onEditClick,
                onExerciseClick = onExerciseClick
            )
        }
    }

    // ── Data from model ───────────────────────────────────────────────────────

    @Test
    fun detailScreen_showsWorkoutTitleInTopBar() {
        setContent()
        composeTestRule.onNodeWithText(workout.title).assertIsDisplayed()
    }

    @Test
    fun detailScreen_showsWorkoutDescription() {
        setContent()
        composeTestRule.onNodeWithText(workout.description).assertIsDisplayed()
    }

    @Test
    fun detailScreen_showsDurationFromModel() {
        setContent()
        composeTestRule.onNodeWithText("${workout.durationMinutes} хв").assertIsDisplayed()
    }

    @Test
    fun detailScreen_showsCaloriesFromModel() {
        setContent()
        composeTestRule.onNodeWithText("${workout.caloriesBurned} ккал").assertIsDisplayed()
    }

    @Test
    fun detailScreen_showsExerciseSectionHeader() {
        setContent()
        composeTestRule
            .onNodeWithText("Вправи (${workout.exercises.size})")
            .assertIsDisplayed()
    }

    @Test
    fun detailScreen_showsExerciseNamesFromModel() {
        setContent()
        workout.exercises.forEach { exercise ->
            composeTestRule.onNodeWithText(exercise.name).assertIsDisplayed()
        }
    }

    @Test
    fun detailScreen_showsMuscleGroupsFromModel() {
        setContent()
        // Use distinct() because multiple exercises can share a muscle group (e.g. "Грудні м'язи").
        // onAllNodesWithText handles the case where the same text appears more than once.
        workout.exercises.map { it.muscleGroup }.distinct().forEach { muscleGroup ->
            composeTestRule.onAllNodesWithText(muscleGroup)[0].assertIsDisplayed()
        }
    }

    // ── Navigation affordances ────────────────────────────────────────────────

    @Test
    fun detailScreen_backButtonIsDisplayed() {
        setContent()
        composeTestRule.onNodeWithContentDescription("Назад").assertIsDisplayed()
    }

    @Test
    fun detailScreen_backButtonClick_triggersOnBack() {
        var backPressed = false
        setContent(onBack = { backPressed = true })

        composeTestRule.onNodeWithContentDescription("Назад").performClick()

        assertTrue(backPressed)
    }

    @Test
    fun detailScreen_tappingExercise_triggersOnExerciseClick() {
        var clickedExercise: com.trainingapp.data.model.Exercise? = null
        setContent(onExerciseClick = { clickedExercise = it })

        composeTestRule
            .onNodeWithText(workout.exercises.first().name)
            .performClick()

        assertEquals(workout.exercises.first().id, clickedExercise?.id)
    }
}
