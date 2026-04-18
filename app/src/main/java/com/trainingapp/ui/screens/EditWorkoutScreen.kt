package com.trainingapp.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.trainingapp.data.model.Exercise
import com.trainingapp.data.model.SyncStatus
import com.trainingapp.data.model.Workout
import java.util.UUID
import com.trainingapp.data.model.WorkoutCategory
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Full edit form for an existing [Workout].
 * Allows changing all workout fields including the date, and managing the
 * exercise list (add / edit / delete individual exercises inline).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditWorkoutScreen(
    workout: Workout,
    onBack: () -> Unit,
    onSave: (Workout) -> Unit
) {
    // ── Workout fields ─────────────────────────────────────────────────
    var title by rememberSaveable { mutableStateOf(workout.title) }
    var description by rememberSaveable { mutableStateOf(workout.description) }
    var duration by rememberSaveable { mutableStateOf(workout.durationMinutes.toString()) }
    var calories by rememberSaveable { mutableStateOf(workout.caloriesBurned.toString()) }
    var isCompleted by rememberSaveable { mutableStateOf(workout.isCompleted) }
    var selectedCategory by remember { mutableStateOf(workout.category) }
    var categoryExpanded by remember { mutableStateOf(false) }
    var selectedDate by remember { mutableStateOf(workout.date) }
    var showDatePicker by remember { mutableStateOf(false) }

    // ── Exercises ──────────────────────────────────────────────────────
    val exercises: SnapshotStateList<Exercise> = remember {
        workout.exercises.toMutableStateList()
    }
    // Index of the exercise being edited: null = none, -1 = new exercise
    var editingIndex by remember { mutableStateOf<Int?>(null) }

    val dateFormatter = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale("uk"))

    val isFormValid = title.isNotBlank() &&
            (duration.toIntOrNull() ?: 0) > 0 &&
            (calories.toIntOrNull() ?: 0) > 0

    // ── Date picker dialog ─────────────────────────────────────────────
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedDate
                .atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        selectedDate = Instant.ofEpochMilli(millis)
                            .atZone(ZoneOffset.UTC).toLocalDate()
                    }
                    showDatePicker = false
                }) { Text("Обрати") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Скасувати") }
            }
        ) { DatePicker(state = datePickerState) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Редагувати тренування") },
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

            // ── Workout fields ─────────────────────────────────────────
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

            // Date — tapping opens DatePickerDialog
            OutlinedTextField(
                value = selectedDate.format(dateFormatter),
                onValueChange = {},
                readOnly = true,
                label = { Text("Дата") },
                trailingIcon = {
                    IconButton(onClick = { showDatePicker = true }) {
                        Icon(Icons.Filled.CalendarMonth, contentDescription = "Обрати дату")
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showDatePicker = true }
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
                    modifier = Modifier.fillMaxWidth().menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = categoryExpanded,
                    onDismissRequest = { categoryExpanded = false }
                ) {
                    WorkoutCategory.entries.forEach { category ->
                        DropdownMenuItem(
                            text = { Text("${category.emoji} ${category.label}") },
                            onClick = { selectedCategory = category; categoryExpanded = false }
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
                    "Тренування виконано",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.alignByBaseline()
                )
                Switch(checked = isCompleted, onCheckedChange = { isCompleted = it })
            }

            HorizontalDivider()

            // ── Exercises section ──────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Вправи (${exercises.size})",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
                if (editingIndex == null) {
                    TextButton(onClick = { editingIndex = -1 }) {
                        Icon(Icons.Filled.Add, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text("Додати вправу")
                    }
                }
            }

            // Existing exercises
            exercises.forEachIndexed { index, exercise ->
                ExerciseEditRow(
                    exercise = exercise,
                    isEditing = editingIndex == index,
                    onEditClick = { editingIndex = index },
                    onDeleteClick = {
                        exercises.removeAt(index)
                        if (editingIndex == index) editingIndex = null
                    },
                    onSave = { updated ->
                        exercises[index] = updated
                        editingIndex = null
                    },
                    onCancel = { editingIndex = null }
                )
            }

            // Inline form for a new exercise
            if (editingIndex == -1) {
                ExerciseForm(
                    initial = null,
                    onSave = { newExercise ->
                        // Assign an ID that doesn't clash with existing ones
                        // Use a UUID-derived ID so it never collides with sample
                        // data (IDs 1–8) or exercises from other workouts in the DB.
                        // The hash is masked to a positive Int.
                        val newId = UUID.randomUUID().hashCode() and Int.MAX_VALUE
                        exercises.add(newExercise.copy(id = newId))
                        editingIndex = null
                    },
                    onCancel = { editingIndex = null }
                )
            }

            Spacer(Modifier.height(8.dp))

            // ── Save workout button ────────────────────────────────────
            Button(
                onClick = {
                    onSave(
                        workout.copy(
                            title = title.trim(),
                            description = description.trim(),
                            durationMinutes = duration.toIntOrNull() ?: 0,
                            caloriesBurned = calories.toIntOrNull() ?: 0,
                            isCompleted = isCompleted,
                            category = selectedCategory,
                            date = selectedDate,
                            exercises = exercises.toList(),
                            syncStatus = SyncStatus.PENDING
                        )
                    )
                    onBack()
                },
                enabled = isFormValid && editingIndex == null,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Зберегти зміни")
            }
        }
    }
}

// ── Exercise row with inline expand-to-edit ────────────────────────────────────

@Composable
private fun ExerciseEditRow(
    exercise: Exercise,
    isEditing: Boolean,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onSave: (Exercise) -> Unit,
    onCancel: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(exercise.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                    Text(
                        exercise.muscleGroup,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    val load = if (exercise.isBodyweight) "власна вага" else "${exercise.weightKg} кг"
                    Text(
                        "${exercise.sets} підх × ${exercise.reps} пов  •  $load",
                        style = MaterialTheme.typography.labelMedium
                    )
                }
                IconButton(onClick = onEditClick) {
                    Icon(Icons.Filled.Edit, contentDescription = "Редагувати вправу")
                }
                IconButton(onClick = onDeleteClick) {
                    Icon(
                        Icons.Filled.Delete,
                        contentDescription = "Видалити вправу",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }

            if (isEditing) {
                Spacer(Modifier.height(12.dp))
                HorizontalDivider()
                Spacer(Modifier.height(12.dp))
                ExerciseForm(
                    initial = exercise,
                    onSave = onSave,
                    onCancel = onCancel
                )
            }
        }
    }
}

// ── Reusable exercise form ─────────────────────────────────────────────────────

@Composable
private fun ExerciseForm(
    initial: Exercise?,
    onSave: (Exercise) -> Unit,
    onCancel: () -> Unit
) {
    var name by rememberSaveable { mutableStateOf(initial?.name ?: "") }
    var muscleGroup by rememberSaveable { mutableStateOf(initial?.muscleGroup ?: "") }
    var sets by rememberSaveable { mutableStateOf(initial?.sets?.toString() ?: "") }
    var reps by rememberSaveable { mutableStateOf(initial?.reps?.toString() ?: "") }
    var weightKg by rememberSaveable { mutableStateOf(initial?.weightKg?.toString() ?: "") }
    var isBodyweight by rememberSaveable { mutableStateOf(initial?.isBodyweight ?: false) }
    var notes by rememberSaveable { mutableStateOf(initial?.notes ?: "") }

    val isValid = name.isNotBlank() &&
            muscleGroup.isNotBlank() &&
            (sets.toIntOrNull() ?: 0) > 0 &&
            (reps.toIntOrNull() ?: 0) > 0 &&
            (isBodyweight || (weightKg.toFloatOrNull() ?: 0f) >= 0f)

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Назва вправи") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = muscleGroup,
            onValueChange = { muscleGroup = it },
            label = { Text("Група м'язів") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedTextField(
                value = sets,
                onValueChange = { sets = it },
                label = { Text("Підходи") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = reps,
                onValueChange = { reps = it },
                label = { Text("Повтори") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Власна вага", style = MaterialTheme.typography.bodyMedium)
            Switch(checked = isBodyweight, onCheckedChange = { isBodyweight = it })
        }

        if (!isBodyweight) {
            OutlinedTextField(
                value = weightKg,
                onValueChange = { weightKg = it },
                label = { Text("Вага (кг)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }

        OutlinedTextField(
            value = notes,
            onValueChange = { notes = it },
            label = { Text("Нотатки (необов'язково)") },
            minLines = 2,
            modifier = Modifier.fillMaxWidth()
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(onClick = onCancel, modifier = Modifier.weight(1f)) {
                Text("Скасувати")
            }
            Button(
                onClick = {
                    onSave(
                        Exercise(
                            id = initial?.id ?: 0,
                            name = name.trim(),
                            muscleGroup = muscleGroup.trim(),
                            sets = sets.toIntOrNull() ?: 0,
                            reps = reps.toIntOrNull() ?: 0,
                            weightKg = if (isBodyweight) 0f else weightKg.toFloatOrNull() ?: 0f,
                            isBodyweight = isBodyweight,
                            notes = notes.trim()
                        )
                    )
                },
                enabled = isValid,
                modifier = Modifier.weight(1f)
            ) {
                Text(if (initial == null) "Додати" else "Зберегти")
            }
        }
    }
}
