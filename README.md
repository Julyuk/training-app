# Training App

An Android fitness tracking application built with **Kotlin** and **Jetpack Compose**.

## Features

- Browse a list of workout sessions with completion status, duration, and calories
- View full workout details including all exercises
- Drill into individual exercise details: sets, reps, weight, and coaching notes
- Personal profile screen with body stats, fitness goal, and weekly progress bar
- Material You dynamic theming with automatic light/dark mode support

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Kotlin |
| UI | Jetpack Compose + Material 3 |
| Navigation | Navigation Compose |
| Min SDK | 26 (Android 8.0) |
| Target SDK | 34 (Android 14) |
| Build | Gradle with Kotlin DSL |

## Project Structure

```
app/src/main/java/com/trainingapp/
├── MainActivity.kt               # App entry point
├── data/
│   ├── model/
│   │   ├── Exercise.kt           # Exercise data model
│   │   ├── Workout.kt            # Workout session data model
│   │   ├── WorkoutCategory.kt    # Category enum (Strength, Cardio, HIIT, Flexibility)
│   │   └── UserProfile.kt        # User profile data model
│   └── SampleData.kt             # In-memory sample data
├── navigation/
│   ├── Screen.kt                 # Sealed class of all navigation routes
│   └── AppNavigation.kt          # NavHost + bottom navigation bar
└── ui/
    ├── screens/
    │   ├── WorkoutListScreen.kt  # Home screen — list of all workouts
    │   ├── WorkoutDetailScreen.kt # Workout details + exercise list
    │   ├── ExerciseDetailScreen.kt # Single exercise detail view
    │   └── ProfileScreen.kt      # User profile & weekly progress
    └── theme/
        ├── Theme.kt              # Material3 theme with dynamic color
        ├── Color.kt              # App color palette
        └── Type.kt               # Typography scale
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

## Screenshots

The app contains four screens navigated via a bottom bar and back stack:

- **Workouts** — scrollable card list with completion indicators
- **Workout Detail** — summary card + tappable exercise rows
- **Exercise Detail** — full parameters and coaching notes
- **Profile** — personal stats, goal, and weekly progress bar
