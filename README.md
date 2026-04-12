# Training App

An Android fitness tracking application built with **Kotlin** and **Jetpack Compose**.

## Features

- Browse a list of workout sessions with completion status, duration, and calories
- Add a new workout — it appears in the list instantly (no restart needed)
- Delete a workout — it disappears from the list instantly (no restart needed)
- View full workout details including all exercises
- Drill into individual exercise details: sets, reps, weight, and coaching notes
- Personal profile screen with body stats, fitness goal, and weekly progress bar
- Data persists across app restarts via local Room database

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Kotlin |
| UI | Jetpack Compose + Material 3 |
| Navigation | Navigation Compose |
| Local storage | Room (SQLite) |
| HTTP client | Retrofit 2 |
| Architecture | ViewModel + StateFlow |
| Min SDK | 26 (Android 8.0) |
| Target SDK | 34 (Android 14) |
| Build | Gradle with Kotlin DSL |

## Sync Strategy: Offline-First

The app uses an **offline-first** strategy because a personal training log must be usable at the gym, outdoors, or anywhere with poor connectivity.

**How it works:**
1. Every **read** goes to the local Room database — instant, reactive `Flow`.
2. Every **write** is saved locally first with `SyncStatus.PENDING`.
3. `syncWithApi()` is called at startup: fetches server state, writes it to local DB, marks records `SYNCED`.
4. On network failure `syncWithApi()` returns silently — local data stays the source of truth.

**When a real server exists:** replace `MockWorkoutApiService` with a Retrofit instance pointed at `/api/v1/`. The `WorkoutRepository` depends only on the `WorkoutApiService` interface, so no other code changes.

## REST API Contract

Base URL: `/api/v1/`

| Method | Path | Returns | Body / Params |
|---|---|---|---|
| GET | `/workouts` | `List<WorkoutDto>` | — |
| GET | `/workouts/{id}` | `WorkoutDto` | Path: `id` (Int) |
| POST | `/workouts` | `WorkoutDto` (created) | Body: `WorkoutDto` |
| DELETE | `/workouts/{id}` | 204 No Content | Path: `id` (Int) |
| GET | `/profile` | `UserProfileDto` | — |

## Project Structure

```
app/src/main/java/com/trainingapp/
├── TrainingApp.kt                    # Application class — DI wiring + DB seed
├── MainActivity.kt                   # App entry point
├── data/
│   ├── model/
│   │   ├── Exercise.kt
│   │   ├── Workout.kt
│   │   ├── WorkoutCategory.kt
│   │   ├── UserProfile.kt
│   │   └── SyncStatus.kt             # PENDING / SYNCED / ERROR
│   ├── local/
│   │   ├── AppDatabase.kt            # Room database singleton
│   │   ├── Converters.kt             # TypeConverters for LocalDate, enums
│   │   ├── dao/WorkoutDao.kt         # save / read list / read one / delete
│   │   └── entity/
│   │       ├── WorkoutEntity.kt
│   │       ├── WorkoutExerciseEntity.kt
│   │       └── WorkoutWithExercises.kt
│   ├── remote/
│   │   ├── WorkoutApiService.kt      # Retrofit interface with @GET/@POST/@DELETE
│   │   ├── MockWorkoutApiService.kt  # In-memory mock with delay
│   │   └── dto/
│   │       ├── WorkoutDto.kt
│   │       └── ExerciseDto.kt
│   ├── repository/
│   │   ├── WorkoutRepository.kt      # Interface
│   │   └── WorkoutRepositoryImpl.kt  # Offline-first implementation
│   └── SampleData.kt                 # Seed data for first launch
├── navigation/
│   ├── Screen.kt
│   └── AppNavigation.kt
└── ui/
    ├── screens/
    │   ├── WorkoutListScreen.kt      # Home — list + FAB (add) + delete icons
    │   ├── AddWorkoutScreen.kt       # Form to create a new workout
    │   ├── WorkoutDetailScreen.kt
    │   ├── ExerciseDetailScreen.kt
    │   └── ProfileScreen.kt
    ├── viewmodel/
    │   ├── WorkoutListViewModel.kt   # add / delete / refresh
    │   └── WorkoutDetailViewModel.kt
    └── theme/
```

## Getting Started

### Prerequisites

- Android Studio Hedgehog or newer
- Android SDK 34
- JDK 17

### Run

1. Clone the repository
2. Open the `training-app` folder in Android Studio
3. Let Gradle sync complete
4. Run on an emulator or physical device (Android 8.0+)
