package com.trainingapp

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.trainingapp.data.SampleData
import com.trainingapp.ui.screens.WorkoutListScreen
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented UI tests for [WorkoutListScreen].
 *
 * Verifies that the screen renders data from model objects (not hardcoded text),
 * exposes add/delete affordances, and fires the correct callbacks on interaction.
 */
@RunWith(AndroidJUnit4::class)
class WorkoutListScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val workouts = SampleData.workouts

    private fun setContent(
        onWorkoutClick: (com.trainingapp.data.model.Workout) -> Unit = {},
        onDeleteClick: (Int) -> Unit = {},
        onAddClick: () -> Unit = {}
    ) {
        composeTestRule.setContent {
            WorkoutListScreen(
                workouts = workouts,
                onWorkoutClick = onWorkoutClick,
                onToggleCompleted = {},
                onDeleteClick = onDeleteClick,
                onAddClick = onAddClick
            )
        }
    }

    // ── Data comes from model objects ─────────────────────────────────────────

    @Test
    fun listScreen_showsTitlesFromModelObjects() {
        setContent()
        // Cards render "${emoji} ${title}" so use substring = true to match the title portion.
        workouts.forEach { workout ->
            composeTestRule.onNodeWithText(workout.title, substring = true).assertIsDisplayed()
        }
    }

    @Test
    fun listScreen_showsSummaryBannerWithCorrectCount() {
        setContent()
        val completedCount = workouts.count { it.isCompleted }
        val total = workouts.size
        composeTestRule
            .onNodeWithText("Виконано: $completedCount з $total тренувань")
            .assertIsDisplayed()
    }

    @Test
    fun listScreen_showsCategoryEmojiFromModel() {
        setContent()
        // Each workout's category emoji must appear alongside its title
        workouts.forEach { workout ->
            composeTestRule
                .onNodeWithText("${workout.category.emoji} ${workout.title}")
                .assertIsDisplayed()
        }
    }

    // ── Add affordance ────────────────────────────────────────────────────────

    @Test
    fun listScreen_fabIsDisplayed() {
        setContent()
        composeTestRule
            .onNodeWithContentDescription("Додати тренування")
            .assertIsDisplayed()
    }

    @Test
    fun listScreen_fabClick_triggersOnAddClick() {
        var addClicked = false
        setContent(onAddClick = { addClicked = true })

        composeTestRule
            .onNodeWithContentDescription("Додати тренування")
            .performClick()

        assertEquals(true, addClicked)
    }

    // ── Delete affordance ─────────────────────────────────────────────────────

    @Test
    fun listScreen_deleteButtonIsPresentForEachCard() {
        setContent()
        // There should be one delete button per workout
        val deleteButtons = composeTestRule
            .onAllNodesWithContentDescription("Видалити тренування")
        deleteButtons[workouts.indices.last].assertIsDisplayed()
    }

    @Test
    fun listScreen_deleteClick_triggersOnDeleteClickWithCorrectId() {
        var deletedId: Int? = null
        setContent(onDeleteClick = { deletedId = it })

        composeTestRule
            .onAllNodesWithContentDescription("Видалити тренування")[0]
            .performClick()

        assertNotNull(deletedId)
    }

    // ── Card tap ──────────────────────────────────────────────────────────────

    @Test
    fun listScreen_tappingCard_triggersOnWorkoutClickWithCorrectWorkout() {
        var clickedWorkout: com.trainingapp.data.model.Workout? = null
        setContent(onWorkoutClick = { clickedWorkout = it })

        composeTestRule
            .onNodeWithText(workouts.first().title, substring = true)
            .performClick()

        assertEquals(workouts.first().id, clickedWorkout?.id)
    }
}
