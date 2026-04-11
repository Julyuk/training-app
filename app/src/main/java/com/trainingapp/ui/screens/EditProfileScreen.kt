package com.trainingapp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.trainingapp.data.model.ActivityLevel
import com.trainingapp.data.model.Sex
import com.trainingapp.data.model.UserProfile

/**
 * Form screen for editing [UserProfile] fields.
 * Changes are only committed when the user taps Save; tapping back discards them.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(
    profile: UserProfile,
    onBack: () -> Unit,
    onSave: (UserProfile) -> Unit
) {
    var name          by remember { mutableStateOf(profile.name) }
    var age           by remember { mutableStateOf(profile.age.toString()) }
    var weightKg      by remember { mutableStateOf(profile.weightKg.toString()) }
    var heightCm      by remember { mutableStateOf(profile.heightCm.toString()) }
    var fitnessGoal   by remember { mutableStateOf(profile.fitnessGoal) }
    var weeklyTarget  by remember { mutableStateOf(profile.weeklyWorkoutTarget.toString()) }
    var selectedSex   by remember { mutableStateOf(profile.sex) }
    var selectedLevel by remember { mutableStateOf(profile.activityLevel) }
    var sexExpanded   by remember { mutableStateOf(false) }
    var levelExpanded by remember { mutableStateOf(false) }

    val isValid = name.isNotBlank() &&
            age.toIntOrNull() != null &&
            weightKg.toFloatOrNull() != null &&
            heightCm.toIntOrNull() != null &&
            fitnessGoal.isNotBlank() &&
            (weeklyTarget.toIntOrNull() ?: 0) > 0

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Редагувати профіль") },
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
        Column(
            modifier = Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Name
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Ім'я") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            // Age + weekly target
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = age,
                    onValueChange = { age = it },
                    label = { Text("Вік") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = weeklyTarget,
                    onValueChange = { weeklyTarget = it },
                    label = { Text("Ціль (трен/тиж)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }

            // Weight + height
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = weightKg,
                    onValueChange = { weightKg = it },
                    label = { Text("Вага (кг)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = heightCm,
                    onValueChange = { heightCm = it },
                    label = { Text("Зріст (см)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }

            // Sex dropdown
            ExposedDropdownMenuBox(
                expanded = sexExpanded,
                onExpandedChange = { sexExpanded = !sexExpanded }
            ) {
                OutlinedTextField(
                    value = selectedSex.label,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Стать") },
                    trailingIcon = {
                        Icon(Icons.Filled.ArrowDropDown, contentDescription = null)
                    },
                    modifier = Modifier.fillMaxWidth().menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = sexExpanded,
                    onDismissRequest = { sexExpanded = false }
                ) {
                    Sex.entries.forEach { sex ->
                        DropdownMenuItem(
                            text = { Text(sex.label) },
                            onClick = { selectedSex = sex; sexExpanded = false }
                        )
                    }
                }
            }

            // Activity level dropdown
            ExposedDropdownMenuBox(
                expanded = levelExpanded,
                onExpandedChange = { levelExpanded = !levelExpanded }
            ) {
                OutlinedTextField(
                    value = selectedLevel.label,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Рівень активності") },
                    supportingText = { Text("Ваша активність поза тренуваннями") },
                    trailingIcon = {
                        Icon(Icons.Filled.ArrowDropDown, contentDescription = null)
                    },
                    modifier = Modifier.fillMaxWidth().menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = levelExpanded,
                    onDismissRequest = { levelExpanded = false }
                ) {
                    ActivityLevel.entries.forEach { level ->
                        DropdownMenuItem(
                            text = { Text(level.label) },
                            onClick = { selectedLevel = level; levelExpanded = false }
                        )
                    }
                }
            }

            // Fitness goal
            OutlinedTextField(
                value = fitnessGoal,
                onValueChange = { fitnessGoal = it },
                label = { Text("Фітнес-ціль") },
                minLines = 2,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = {
                    onSave(
                        profile.copy(
                            name = name.trim(),
                            age = age.toInt(),
                            weightKg = weightKg.toFloat(),
                            heightCm = heightCm.toInt(),
                            fitnessGoal = fitnessGoal.trim(),
                            weeklyWorkoutTarget = weeklyTarget.toInt(),
                            sex = selectedSex,
                            activityLevel = selectedLevel
                        )
                    )
                    onBack()
                },
                enabled = isValid,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Зберегти")
            }
        }
    }
}
