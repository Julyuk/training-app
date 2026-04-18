package com.trainingapp.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.trainingapp.data.model.SyncStatus
import com.trainingapp.data.model.Workout
import com.trainingapp.data.model.WorkoutCategory
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Displays all workout sessions as a scrollable list.
 * Each card shows the workout title, date, category, duration and completion status.
 * Tapping a card triggers [onWorkoutClick] to navigate to the detail screen.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun WorkoutListScreen(
    workouts: List<Workout>,
    onWorkoutClick: (Workout) -> Unit,
    onToggleCompleted: (Int) -> Unit,
    onDeleteClick: (Int) -> Unit,
    onAddClick: () -> Unit,
    /** Categories that belong to at least one joined challenge — drives the 🎯 badge. */
    joinedChallengeCategories: Set<WorkoutCategory> = emptySet()
) {
    val dateFormatter = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale("uk"))
    val completedCount = workouts.count { it.isCompleted }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Мої тренування") },
                actions = {
                    IconButton(onClick = onAddClick) {
                        Icon(Icons.Filled.Add, contentDescription = "Додати тренування")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            // Summary banner
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Виконано: $completedCount з ${workouts.size} тренувань",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                )
            }

            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(workouts, key = { it.id }) { workout ->
                    WorkoutCard(
                        workout = workout,
                        dateText = workout.date.format(dateFormatter),
                        hasActiveChallenge = workout.category in joinedChallengeCategories,
                        onClick = { onWorkoutClick(workout) },
                        onToggleCompleted = { onToggleCompleted(workout.id) },
                        onDeleteClick = { onDeleteClick(workout.id) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun WorkoutCard(
    workout: Workout,
    dateText: String,
    hasActiveChallenge: Boolean,
    onClick: () -> Unit,
    onToggleCompleted: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        // Use a Column layout so icons never fight with the 48 dp
        // minimum touch-target that IconButton enforces internally.
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {

            // ── Title row: title on the left, delete icon on the right ──────
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${workout.category.emoji} ${workout.title}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                // Plain Icon + clickable — no IconButton, no 48 dp minimum
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = "Видалити тренування",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier
                        .size(18.dp)
                        .clickable { onDeleteClick() }
                )
            }

            Spacer(Modifier.height(2.dp))
            Text(
                text = dateText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(6.dp))

            // ── Bottom row: toggle icon + chips ─────────────────────────────
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = if (workout.isCompleted) Icons.Filled.CheckCircle
                                  else Icons.Filled.RadioButtonUnchecked,
                    contentDescription = if (workout.isCompleted) "Позначити як невиконане"
                                         else "Позначити як виконане",
                    tint = if (workout.isCompleted) MaterialTheme.colorScheme.primary
                           else MaterialTheme.colorScheme.outline,
                    modifier = Modifier
                        .size(20.dp)
                        .clickable { onToggleCompleted() }
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    InfoChip("⏱ ${workout.durationMinutes} хв")
                    InfoChip("🔥 ${workout.caloriesBurned} ккал")
                    if (workout.exercises.isNotEmpty()) {
                        InfoChip("🏋 ${workout.exercises.size} вправ")
                    }
                    if (hasActiveChallenge) {
                        InfoChip("🎯 Виклик")
                    }
                    // Sync status badge — visible only for records not yet uploaded
                    when (workout.syncStatus) {
                        SyncStatus.PENDING -> InfoChip("⏳ Синхронізація")
                        SyncStatus.ERROR   -> InfoChip("❌ Помилка")
                        SyncStatus.SYNCED  -> Unit
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoChip(text: String) {
    Surface(
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}
