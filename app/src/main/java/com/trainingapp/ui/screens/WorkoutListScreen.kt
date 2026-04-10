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
import com.trainingapp.data.model.Workout
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Displays all workout sessions as a scrollable list.
 * Each card shows the workout title, date, category, duration and completion status.
 * Tapping a card triggers [onWorkoutClick] to navigate to the detail screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutListScreen(
    workouts: List<Workout>,
    onWorkoutClick: (Workout) -> Unit,
    onToggleCompleted: (Int) -> Unit,
    onDeleteClick: (Int) -> Unit,
    onAddClick: () -> Unit
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
                        onClick = { onWorkoutClick(workout) },
                        onToggleCompleted = { onToggleCompleted(workout.id) },
                        onDeleteClick = { onDeleteClick(workout.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun WorkoutCard(
    workout: Workout,
    dateText: String,
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
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Completion toggle
            IconButton(
                onClick = onToggleCompleted,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = if (workout.isCompleted) Icons.Filled.CheckCircle
                                  else Icons.Filled.RadioButtonUnchecked,
                    contentDescription = if (workout.isCompleted) "Позначити як невиконане"
                                         else "Позначити як виконане",
                    tint = if (workout.isCompleted) MaterialTheme.colorScheme.primary
                           else MaterialTheme.colorScheme.outline,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${workout.category.emoji} ${workout.title}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = dateText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    InfoChip("⏱ ${workout.durationMinutes} хв")
                    InfoChip("🔥 ${workout.caloriesBurned} ккал")
                    InfoChip("🏋 ${workout.exercises.size} вправ")
                }
            }

            IconButton(onClick = onDeleteClick) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = "Видалити тренування",
                    tint = MaterialTheme.colorScheme.error
                )
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
