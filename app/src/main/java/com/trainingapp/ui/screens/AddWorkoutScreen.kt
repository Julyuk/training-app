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
import com.trainingapp.data.model.WorkoutCategory

/**
 * Form screen that lets the user create a new workout session.
 * On [onSave] the caller persists the record; the list screen updates
 * automatically because the ViewModel exposes a Room Flow.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddWorkoutScreen(
    onBack: () -> Unit,
    onSave: (
        title: String,
        description: String,
        durationMinutes: Int,
        caloriesBurned: Int,
        isCompleted: Boolean,
        category: WorkoutCategory
    ) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var duration by remember { mutableStateOf("") }
    var calories by remember { mutableStateOf("") }
    var isCompleted by remember { mutableStateOf(true) }
    var selectedCategory by remember { mutableStateOf(WorkoutCategory.STRENGTH) }
    var categoryExpanded by remember { mutableStateOf(false) }

    val isFormValid = title.isNotBlank() &&
            (duration.toIntOrNull() ?: 0) > 0 &&
            (calories.toIntOrNull() ?: 0) > 0

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Нове тренування") },
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
                value = title,
                onValueChange = { title = it },
                label = { Text("Назва тренування") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Опис (необов'язково)") },
                minLines = 2,
                modifier = Modifier.fillMaxWidth()
            )

            // Category dropdown
            ExposedDropdownMenuBox(
                expanded = categoryExpanded,
                onExpandedChange = { categoryExpanded = !categoryExpanded }
            ) {
                OutlinedTextField(
                    value = "${selectedCategory.emoji} ${selectedCategory.label}",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Категорія") },
                    trailingIcon = {
                        Icon(Icons.Filled.ArrowDropDown, contentDescription = null)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = categoryExpanded,
                    onDismissRequest = { categoryExpanded = false }
                ) {
                    WorkoutCategory.entries.forEach { category ->
                        DropdownMenuItem(
                            text = { Text("${category.emoji} ${category.label}") },
                            onClick = {
                                selectedCategory = category
                                categoryExpanded = false
                            }
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = duration,
                    onValueChange = { duration = it },
                    label = { Text("Тривалість (хв)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = calories,
                    onValueChange = { calories = it },
                    label = { Text("Калорії") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Тренування виконано",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.alignByBaseline()
                )
                Switch(
                    checked = isCompleted,
                    onCheckedChange = { isCompleted = it }
                )
            }

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = {
                    if (isFormValid) {
                        onSave(
                            title.trim(),
                            description.trim(),
                            duration.toInt(),
                            calories.toInt(),
                            isCompleted,
                            selectedCategory
                        )
                        onBack()
                    }
                },
                enabled = isFormValid,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Зберегти тренування")
            }
        }
    }
}
