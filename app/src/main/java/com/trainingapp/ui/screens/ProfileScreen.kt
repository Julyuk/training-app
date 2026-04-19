package com.trainingapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.trainingapp.data.model.UserIdentity
import com.trainingapp.data.model.UserPhysical
import com.trainingapp.data.model.UserPreferences
import com.trainingapp.data.model.Workout
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters
import kotlin.math.roundToInt

private val MONTH_ABBR = listOf(
    "січ", "лют", "бер", "квіт", "трав", "черв",
    "лип", "серп", "вер", "жовт", "лист", "груд"
)

private fun LocalDate.monthAbbr() = MONTH_ABBR[monthValue - 1]

/**
 * Profile screen.
 *
 * [allWorkouts] — the full live list from the DB.
 * Week filtering, navigation, and day selection are all local state here
 * so the screen is fully self-contained and never requires a DB round-trip
 * when the user switches weeks.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    identity: UserIdentity,
    physical: UserPhysical,
    preferences: UserPreferences,
    allWorkouts: List<Workout>,
    onEditClick: () -> Unit,
    onSecurityClick: () -> Unit = {}
) {
    val today = remember { LocalDate.now() }

    // ── Week navigation ────────────────────────────────────────────────
    // weekOffset=0 → current week; -1 → last week; etc.
    var weekOffset by rememberSaveable { mutableStateOf(0) }

    val weekStart = remember(weekOffset) {
        today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            .plusWeeks(weekOffset.toLong())
    }
    val weekEnd = remember(weekOffset) { weekStart.plusDays(6) }

    // ── Selected day (stored as epoch-day Long for rememberSaveable) ───
    var selectedDayEpoch by rememberSaveable { mutableStateOf(today.toEpochDay()) }
    val selectedDay = LocalDate.ofEpochDay(selectedDayEpoch)

    // When the displayed week changes, keep the selection inside it:
    // snap to today if the current week contains today, else snap to Monday.
    LaunchedEffect(weekStart) {
        val sd = LocalDate.ofEpochDay(selectedDayEpoch)
        if (sd.isBefore(weekStart) || sd.isAfter(weekEnd)) {
            selectedDayEpoch = if (!today.isBefore(weekStart) && !today.isAfter(weekEnd))
                today.toEpochDay()
            else
                weekStart.toEpochDay()
        }
    }

    // ── Workouts for the displayed week ───────────────────────────────
    val thisWeekWorkouts = remember(allWorkouts, weekStart, weekEnd) {
        allWorkouts.filter { !it.date.isBefore(weekStart) && !it.date.isAfter(weekEnd) }
    }
    val completedThisWeek = thisWeekWorkouts.count { it.isCompleted }

    // ── Calorie goals (Total Daily Energy Expenditure model) ──────────
    // Tracks Base Metabolism + Exercise — same model as Fitbit / Apple Health.
    // Numbers are comparable to the familiar 2 000–2 500 kcal/day norm.
    //
    // BMR  = Mifflin-St Jeor with sex-specific offset:
    //          Male   → +5     Female → −161     Unspecified → −78 (average)
    // TDEE = BMR × user's activity-level multiplier (1.2 – 1.9)
    val bmr = 10f * physical.weightKg +
            6.25f * physical.heightCm -
            5f * physical.age +
            physical.sex.bmrOffset
    val dailyCalorieGoal  = (bmr * preferences.activityLevel.multiplier).roundToInt()
        .coerceAtLeast(1200)
    val weeklyCalorieGoal = dailyCalorieGoal * 7

    // How many days of the displayed week have already passed (including today)?
    // Past week → all 7 days.  Future week → 0 days (nothing has been burned yet).
    // Current week → days from Monday up to and including today.
    val daysElapsedInWeek = when {
        weekOffset < 0 -> 7
        weekOffset > 0 -> 0
        else -> (ChronoUnit.DAYS.between(weekStart, today).toInt() + 1)
            .coerceIn(1, 7)
    }

    // Calories burned = base metabolism for elapsed days + completed workout calories
    val workoutCalsThisWeek = thisWeekWorkouts
        .filter { it.isCompleted }.sumOf { it.caloriesBurned }
    val caloriesBurnedThisWeek = bmr.roundToInt() * daysElapsedInWeek + workoutCalsThisWeek

    // For the selected day: include full daily BMR only if the day has already happened
    val workoutCalsSelectedDay = allWorkouts
        .filter { it.date == selectedDay && it.isCompleted }.sumOf { it.caloriesBurned }
    val caloriesBurnedSelectedDay =
        (if (!selectedDay.isAfter(today)) bmr.roundToInt() else 0) + workoutCalsSelectedDay

    // ── BMI ────────────────────────────────────────────────────────────
    val heightM = physical.heightCm / 100f
    val bmi = if (heightM > 0f) physical.weightKg / (heightM * heightM) else 0f

    // ── Progress fractions ─────────────────────────────────────────────
    val workoutRaw = if (preferences.weeklyWorkoutTarget > 0)
        completedThisWeek.toFloat() / preferences.weeklyWorkoutTarget else 0f
    val weekCalRaw = if (weeklyCalorieGoal > 0)
        caloriesBurnedThisWeek.toFloat() / weeklyCalorieGoal else 0f
    val dayCalRaw = if (dailyCalorieGoal > 0)
        caloriesBurnedSelectedDay.toFloat() / dailyCalorieGoal else 0f

    // ── Labels ─────────────────────────────────────────────────────────
    val weekLabel = if (weekStart.month == weekEnd.month)
        "${weekStart.dayOfMonth}–${weekEnd.dayOfMonth} ${weekEnd.monthAbbr()}"
    else
        "${weekStart.dayOfMonth} ${weekStart.monthAbbr()} – ${weekEnd.dayOfMonth} ${weekEnd.monthAbbr()}"

    val selectedDayLabel = when (selectedDay) {
        today -> "Сьогодні"
        else -> "${selectedDay.dayOfMonth} ${selectedDay.monthAbbr()}"
    }

    val selectedDayWorkoutsAll = allWorkouts.filter { it.date == selectedDay }
    val selectedDayCompleted = selectedDayWorkoutsAll.count { it.isCompleted }

    // ──────────────────────────────────────────────────────────────────
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Профіль") },
                actions = {
                    IconButton(onClick = onSecurityClick) {
                        Icon(Icons.Filled.Lock, contentDescription = "Налаштування безпеки")
                    }
                    IconButton(onClick = onEditClick) {
                        Icon(Icons.Filled.Edit, contentDescription = "Редагувати профіль")
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
            // ── Avatar + name ──────────────────────────────────────────
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(24.dp),
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
                            modifier = Modifier.padding(16.dp).fillMaxSize(),
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(
                        identity.name,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    if (identity.isPremium) {
                        Spacer(Modifier.height(4.dp))
                        Badge { Text("Premium") }
                    }
                }
            }

            // ── Body stats ─────────────────────────────────────────────
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    SectionTitle("Особисті дані")
                    Spacer(Modifier.height(12.dp))
                    ProfileRow("Вік", "${physical.age} років")
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    ProfileRow("Вага", "${physical.weightKg} кг")
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    ProfileRow("Зріст", "${physical.heightCm} см")
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    ProfileRow("ІМТ", String.format("%.1f", bmi))
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    ProfileRow("Стать", physical.sex.label)
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    ProfileRowStacked("Рівень активності", preferences.activityLevel.label)
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    ProfileRow("Ціль на тиждень", "${preferences.weeklyWorkoutTarget} трен.")
                }
            }

            // ── Fitness goal ───────────────────────────────────────────
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    SectionTitle("Мета")
                    Spacer(Modifier.height(8.dp))
                    Text(preferences.fitnessGoal, style = MaterialTheme.typography.bodyMedium)
                }
            }

            // ── Weekly progress ────────────────────────────────────────
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    SectionTitle("Тижневий прогрес")
                    Spacer(Modifier.height(8.dp))

                    // Week navigation row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        IconButton(onClick = { weekOffset-- }) {
                            Icon(
                                Icons.Filled.KeyboardArrowLeft,
                                contentDescription = "Попередній тиждень"
                            )
                        }
                        Text(
                            text = weekLabel,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        IconButton(onClick = { weekOffset++ }) {
                            Icon(
                                Icons.Filled.KeyboardArrowRight,
                                contentDescription = "Наступний тиждень"
                            )
                        }
                    }

                    Spacer(Modifier.height(4.dp))

                    // Calendar strip
                    WeekCalendarRow(
                        weekStart = weekStart,
                        today = today,
                        thisWeekWorkouts = thisWeekWorkouts,
                        selectedDay = selectedDay,
                        onDaySelected = { selectedDayEpoch = it.toEpochDay() }
                    )

                    Spacer(Modifier.height(6.dp))

                    // Calorie trend bars (aligned under each day circle)
                    CalorieTrendChart(
                        weekStart = weekStart,
                        allWorkouts = allWorkouts,
                        selectedDay = selectedDay,
                        dailyGoal = dailyCalorieGoal,
                        onDaySelected = { selectedDayEpoch = it.toEpochDay() }
                    )

                    Spacer(Modifier.height(10.dp))

                    // Selected-day detail chip
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = selectedDayLabel,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.weight(1f)
                            )
                            if (selectedDayWorkoutsAll.isEmpty()) {
                                Text(
                                    "Немає тренувань",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else {
                                Text(
                                    "$selectedDayCompleted трен.",
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Text(
                                    "•",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    "$caloriesBurnedSelectedDay ккал",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    // Workout count progress
                    ProgressRow(
                        label = "$completedThisWeek з ${preferences.weeklyWorkoutTarget} тренувань",
                        percent = (workoutRaw * 100).roundToInt(),
                        fraction = workoutRaw.coerceIn(0f, 1f)
                    )

                    Spacer(Modifier.height(12.dp))

                    // Weekly calorie progress
                    ProgressRow(
                        label = "Тиждень: $caloriesBurnedThisWeek з $weeklyCalorieGoal ккал",
                        percent = (weekCalRaw * 100).roundToInt(),
                        fraction = weekCalRaw.coerceIn(0f, 1f)
                    )

                    Spacer(Modifier.height(12.dp))

                    // Daily calorie progress (tracks selected day)
                    ProgressRow(
                        label = "$selectedDayLabel: $caloriesBurnedSelectedDay з $dailyCalorieGoal ккал",
                        percent = (dayCalRaw * 100).roundToInt(),
                        fraction = dayCalRaw.coerceIn(0f, 1f)
                    )

                    Spacer(Modifier.height(12.dp))
                    HorizontalDivider()
                    Spacer(Modifier.height(8.dp))

                    // ── Calorie explanation hints ──────────────────────
                    CalorieHint(
                        icon = "🔥",
                        title = "Базовий метаболізм (BMR): ${bmr.roundToInt()} ккал/день",
                        body = "Скільки калорій ваше тіло спалює у повному спокої — на дихання, " +
                                "серцебиття та підтримання температури. Не залежить від активності."
                    )
                    Spacer(Modifier.height(6.dp))
                    CalorieHint(
                        icon = "⚡",
                        title = "Денна норма (TDEE): $dailyCalorieGoal ккал/день",
                        body = "BMR × коефіцієнт активності (${preferences.activityLevel.multiplier}). " +
                                "Це ваш реальний добовий витрата з урахуванням способу життя. " +
                                "Їжте менше — схуднете, більше — наберете вагу."
                    )
                }
            }
        }
    }
}

// ── Calendar strip ─────────────────────────────────────────────────────────────

@Composable
private fun WeekCalendarRow(
    weekStart: LocalDate,
    today: LocalDate,
    thisWeekWorkouts: List<Workout>,
    selectedDay: LocalDate,
    onDaySelected: (LocalDate) -> Unit
) {
    val dayLabels = listOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Нд")

    Row(modifier = Modifier.fillMaxWidth()) {
        for (offset in 0..6) {
            val day = weekStart.plusDays(offset.toLong())
            val isToday = day == today
            val isSelected = day == selectedDay
            val workoutsOnDay = thisWeekWorkouts.filter { it.date == day }
            val hasCompleted = workoutsOnDay.any { it.isCompleted }
            val hasScheduled = workoutsOnDay.isNotEmpty() && !hasCompleted

            val bgColor = when {
                hasCompleted -> MaterialTheme.colorScheme.primary
                hasScheduled -> MaterialTheme.colorScheme.secondaryContainer
                else -> MaterialTheme.colorScheme.surfaceVariant
            }
            val numColor = when {
                hasCompleted -> MaterialTheme.colorScheme.onPrimary
                hasScheduled -> MaterialTheme.colorScheme.onSecondaryContainer
                else -> MaterialTheme.colorScheme.onSurfaceVariant
            }

            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = dayLabels[offset],
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isToday) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(bgColor)
                        .then(
                            when {
                                isSelected ->
                                    Modifier.border(2.dp, MaterialTheme.colorScheme.tertiary, CircleShape)
                                isToday ->
                                    Modifier.border(2.dp, MaterialTheme.colorScheme.outline, CircleShape)
                                else -> Modifier
                            }
                        )
                        .clickable { onDaySelected(day) }
                ) {
                    Text(
                        text = day.dayOfMonth.toString(),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (isToday || isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = numColor,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

// ── Calorie trend chart ────────────────────────────────────────────────────────

@Composable
private fun CalorieTrendChart(
    weekStart: LocalDate,
    allWorkouts: List<Workout>,
    selectedDay: LocalDate,
    dailyGoal: Int,
    onDaySelected: (LocalDate) -> Unit
) {
    val calsByDay = remember(allWorkouts, weekStart) {
        (0..6).map { offset ->
            val day = weekStart.plusDays(offset.toLong())
            val cals = allWorkouts
                .filter { it.date == day && it.isCompleted }
                .sumOf { it.caloriesBurned }
            day to cals
        }
    }

    val maxCals = maxOf(
        calsByDay.maxOfOrNull { it.second } ?: 0,
        dailyGoal,
        1
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        calsByDay.forEach { (day, cals) ->
            val fillFraction = cals.toFloat() / maxCals
            val isSelected = day == selectedDay

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(horizontal = 3.dp)
                    .clickable { onDaySelected(day) },
                contentAlignment = Alignment.BottomCenter
            ) {
                // Track
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                )
                // Fill
                if (fillFraction > 0f) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(fillFraction)
                            .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.tertiary
                                else MaterialTheme.colorScheme.primary
                            )
                    )
                }
            }
        }
    }
}

// ── Shared composables ─────────────────────────────────────────────────────────

@Composable
private fun ProgressRow(label: String, percent: Int, fraction: Float) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = "$percent%",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary
        )
    }
    Spacer(Modifier.height(4.dp))
    LinearProgressIndicator(
        progress = { fraction },
        modifier = Modifier.fillMaxWidth().height(8.dp),
        trackColor = MaterialTheme.colorScheme.surfaceVariant
    )
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
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun ProfileRowStacked(label: String, value: String) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun CalorieHint(icon: String, title: String, body: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(icon, style = MaterialTheme.typography.bodyMedium)
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = body,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
