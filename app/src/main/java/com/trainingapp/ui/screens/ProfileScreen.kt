package com.trainingapp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.trainingapp.data.SampleData
import com.trainingapp.data.model.UserProfile
import kotlin.math.roundToInt

/**
 * Displays the user's personal profile information and fitness statistics.
 * Weekly progress is calculated from [SampleData.workouts] completed this week.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(profile: UserProfile) {
    val completedThisWeek = SampleData.workouts.count { it.isCompleted }
    val progressFraction = (completedThisWeek.toFloat() / profile.weeklyWorkoutTarget).coerceIn(0f, 1f)
    val bmi = profile.weightKg / ((profile.heightCm / 100f) * (profile.heightCm / 100f))

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Профіль") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Avatar + name
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(80.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Person,
                            contentDescription = null,
                            modifier = Modifier
                                .padding(16.dp)
                                .fillMaxSize(),
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = profile.name,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    if (profile.isPremium) {
                        Spacer(Modifier.height(4.dp))
                        Badge { Text("Premium") }
                    }
                }
            }

            // Body stats
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    SectionTitle("Особисті дані")
                    Spacer(Modifier.height(12.dp))
                    ProfileRow("Вік", "${profile.age} років")
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    ProfileRow("Вага", "${profile.weightKg} кг")
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    ProfileRow("Зріст", "${profile.heightCm} см")
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    ProfileRow("ІМТ", String.format("%.1f", bmi))
                }
            }

            // Fitness goal
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    SectionTitle("Мета")
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = profile.fitnessGoal,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            // Weekly progress
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    SectionTitle("Тижневий прогрес")
                    Spacer(Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "$completedThisWeek з ${profile.weeklyWorkoutTarget} тренувань",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "${(progressFraction * 100).roundToInt()}%",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { progressFraction },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp),
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary
    )
}

@Composable
private fun ProfileRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium
        )
    }
}
