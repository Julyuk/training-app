package com.trainingapp

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.trainingapp.navigation.AppNavigation
import com.trainingapp.ui.theme.TrainingAppTheme

// AppCompatActivity extends FragmentActivity, required by BiometricPrompt.
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TrainingAppTheme {
                AppNavigation()
            }
        }
    }
}
