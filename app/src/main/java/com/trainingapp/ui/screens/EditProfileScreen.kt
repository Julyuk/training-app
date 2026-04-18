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
import com.trainingapp.data.model.UserIdentity
import com.trainingapp.data.model.UserPhysical
import com.trainingapp.data.model.UserPreferences

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(
    identity: UserIdentity,
    physical: UserPhysical,
    preferences: UserPreferences,
    onBack: () -> Unit,
    onSave: (UserIdentity, UserPhysical, UserPreferences) -> Unit
) {
    var name          by remember { mutableStateOf(identity.name) }
    var age           by remember { mutableStateOf(physical.age.toString()) }
    var weightKg      by remember { mutableStateOf(physical.weightKg.toString()) }
    var heightCm      by remember { mutableStateOf(physical.heightCm.toString()) }
    var fitnessGoal   by remember { mutableStateOf(preferences.fitnessGoal) }
    var weeklyTarget  by remember { mutableStateOf(preferences.weeklyWorkoutTarget.toString()) }
    var selectedSex   by remember { mutableStateOf(physical.sex) }
    var selectedLevel by remember { mutableStateOf(preferences.activityLevel) }
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
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Ім'я") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

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
                        identity.copy(name = name.trim()),
                        physical.copy(
                            age = age.toIntOrNull() ?: 0,
                            weightKg = weightKg.toFloatOrNull() ?: 0f,
                            heightCm = heightCm.toIntOrNull() ?: 0,
                            sex = selectedSex
                        ),
                        preferences.copy(
                            fitnessGoal = fitnessGoal.trim(),
                            weeklyWorkoutTarget = weeklyTarget.toIntOrNull() ?: 0,
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
