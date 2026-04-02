package com.trainingapp.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForwardIos
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.trainingapp.data.model.Exercise
import com.trainingapp.data.model.Workout
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Shows the full details of a single [Workout]: metadata at the top and
 * the list of exercises below. Tapping an exercise navigates to [ExerciseDetailScreen].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutDetailScreen(
    workout: Workout,
    onBack: () -> Unit,
    onExerciseClick: (Exercise) -> Unit
) {
    val dateFormatter = DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", Locale("uk"))

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(workout.title, maxLines = 1) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Workout summary card
            item {
                WorkoutSummaryCard(workout = workout, dateText = workout.date.format(dateFormatter))
            }

            // Section header
            item {
                Text(
                    text = "Вправи (${workout.exercises.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            // Exercise rows
            items(workout.exercises, key = { it.id }) { exercise ->
                ExerciseRow(
                    exercise = exercise,
                    onClick = { onExerciseClick(exercise) }
                )
            }
        }
    }
}

@Composable
private fun WorkoutSummaryCard(workout: Workout, dateText: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "${workout.category.emoji} ${workout.category.label}",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = workout.description,
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = dateText.replaceFirstChar { it.uppercase() },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                StatItem(label = "Тривалість", value = "${workout.durationMinutes} хв")
                StatItem(label = "Калорії", value = "${workout.caloriesBurned} ккал")
                StatItem(label = "Статус", value = if (workout.isCompleted) "Виконано" else "Заплановано")
            }
        }
    }
}

@Composable
private fun StatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ExerciseRow(exercise: Exercise, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = exercise.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = exercise.muscleGroup,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(4.dp))
                val loadText = if (exercise.isBodyweight) "власна вага"
                               else "${exercise.weightKg} кг"
                Text(
                    text = "${exercise.sets} підх × ${exercise.reps} пов   •   $loadText",
                    style = MaterialTheme.typography.labelMedium
                )
            }
            Icon(
                imageVector = Icons.Filled.ArrowForwardIos,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
