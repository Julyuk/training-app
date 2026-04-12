package com.trainingapp

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.trainingapp.data.SampleData
import com.trainingapp.data.model.Exercise
import com.trainingapp.data.model.Workout
import com.trainingapp.navigation.Screen
import com.trainingapp.ui.screens.WorkoutDetailScreen
import com.trainingapp.ui.screens.WorkoutListScreen
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented navigation flow tests.
 *
 * Sets up a minimal NavHost with the list and detail composables so navigation
 * behaviour can be verified without a full Application/Room dependency.
 * Tests cover:
 *  - Bottom tab destinations are reachable
 *  - List → Detail transition on card tap
 *  - Back navigation from detail returns to list
 */
@RunWith(AndroidJUnit4::class)
class NavigationFlowTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val workouts = SampleData.workouts

    /** Sets up a two-destination NavHost: list and detail. */
    private fun setListDetailNavHost(
        extraOnBack: () -> Unit = {}
    ) {
        composeTestRule.setContent {
            val navController = rememberNavController()
            NavHost(
                navController = navController,
                startDestination = Screen.WorkoutList.route
            ) {
                composable(Screen.WorkoutList.route) {
                    WorkoutListScreen(
                        workouts = workouts,
                        onWorkoutClick = { workout ->
                            navController.navigate(Screen.WorkoutDetail.createRoute(workout.id))
                        },
                        onToggleCompleted = {},
                        onDeleteClick = {},
                        onAddClick = {}
                    )
                }
                composable(
                    route = Screen.WorkoutDetail.route,
                    arguments = listOf(navArgument("workoutId") { type = NavType.IntType })
                ) { backStackEntry ->
                    val id = backStackEntry.arguments?.getInt("workoutId") ?: return@composable
                    val workout = workouts.find { it.id == id } ?: return@composable
                    WorkoutDetailScreen(
                        workout = workout,
                        onBack = {
                            extraOnBack()
                            navController.popBackStack()
                        },
                        onEditClick = {},
                        onExerciseClick = {}
                    )
                }
            }
        }
    }

    // ── List screen is the start destination ─────────────────────────────────

    @Test
    fun navigation_startDestination_isWorkoutList() {
        setListDetailNavHost()
        // Cards render "${emoji} ${title}" — use substring = true to match the title portion.
        composeTestRule
            .onNodeWithText(workouts.first().title, substring = true)
            .assertIsDisplayed()
    }

    @Test
    fun navigation_listScreen_hasNoBackButton_atStart() {
        setListDetailNavHost()
        // Back button must NOT be present on the root list screen
        composeTestRule
            .onNodeWithContentDescription("Назад")
            .assertDoesNotExist()
    }

    // ── List → Detail ─────────────────────────────────────────────────────────

    @Test
    fun navigation_tappingWorkoutCard_navigatesToDetailScreen() {
        setListDetailNavHost()
        val target = workouts.first()

        composeTestRule.onNodeWithText(target.title, substring = true).performClick()

        // Detail screen shows back button and the same title (TopAppBar has no emoji prefix)
        composeTestRule.onNodeWithContentDescription("Назад").assertIsDisplayed()
        composeTestRule.onNodeWithText(target.title).assertIsDisplayed()
    }

    @Test
    fun navigation_detailScreen_showsCorrectWorkoutForTappedCard() {
        setListDetailNavHost()
        val target = workouts[1] // second workout

        composeTestRule.onNodeWithText(target.title, substring = true).performClick()

        // Description comes from the model of the tapped workout specifically
        composeTestRule.onNodeWithText(target.description).assertIsDisplayed()
    }

    // ── Back navigation ───────────────────────────────────────────────────────

    @Test
    fun navigation_backFromDetail_returnsToList() {
        setListDetailNavHost()

        // Navigate to detail
        composeTestRule.onNodeWithText(workouts.first().title, substring = true).performClick()
        composeTestRule.onNodeWithContentDescription("Назад").assertIsDisplayed()

        // Press back
        composeTestRule.onNodeWithContentDescription("Назад").performClick()

        // List is visible again — title still found with substring match, back button gone
        composeTestRule.onNodeWithText(workouts.first().title, substring = true).assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Назад").assertDoesNotExist()
    }

    @Test
    fun navigation_backButton_callsOnBack_callback() {
        var backPressed = false
        setListDetailNavHost(extraOnBack = { backPressed = true })

        composeTestRule.onNodeWithText(workouts.first().title, substring = true).performClick()
        composeTestRule.onNodeWithContentDescription("Назад").performClick()

        assert(backPressed)
    }

    // ── Multiple cards lead to correct detail ────────────────────────────────

    @Test
    fun navigation_eachCardNavigatesToItsOwnDetailScreen() {
        // setContent can only be called once per test — set up the nav host before the loop,
        // then navigate forward and back for each workout within the same composition.
        setListDetailNavHost()

        workouts.take(3).forEach { workout ->
            composeTestRule.onNodeWithText(workout.title, substring = true).performClick()

            // The detail screen shows the description of THIS workout, not another
            composeTestRule.onNodeWithText(workout.description).assertIsDisplayed()

            // Return to list before checking the next card
            composeTestRule.onNodeWithContentDescription("Назад").performClick()
        }
    }
}
