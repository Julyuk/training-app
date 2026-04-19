package com.trainingapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.trainingapp.data.biometric.AuthState
import com.trainingapp.data.biometric.BiometricType

/**
 * Full-screen lock overlay shown when the app auto-locks after inactivity.
 *
 * The screen blocks all user interaction with the rest of the app.
 * It is displayed as a [Box] overlay in [AppNavigation] rather than a
 * navigation destination, so the back button cannot dismiss it.
 *
 * When [authState] transitions to [AuthState.Success], [onUnlocked] is called
 * and the caller removes this composable from the hierarchy.
 *
 * @param biometricType  Sensor type shown in the button label.
 * @param authState      Current authentication state — drives the status message.
 * @param onAuthenticate Called when the user taps the biometric button.
 * @param onUnlocked     Called once [AuthState.Success] is observed so the caller
 *                       can hide this screen and reset the auth state.
 */
@Composable
fun LockScreen(
    biometricType: BiometricType,
    authState: AuthState,
    onAuthenticate: () -> Unit,
    onUnlocked: () -> Unit
) {
    LaunchedEffect(authState) {
        if (authState is AuthState.Success) onUnlocked()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Lock,
                contentDescription = null,
                modifier = Modifier.size(72.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            Text(
                text = "Застосунок заблоковано",
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center
            )

            Text(
                text = "Підтвердіть свою особу, щоб продовжити",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (biometricType != BiometricType.NONE) {
                Button(
                    onClick = onAuthenticate,
                    enabled = authState !is AuthState.Authenticating
                ) {
                    Text("${biometricType.icon()} ${biometricType.displayName()}")
                }
            }

            // Status feedback
            when (authState) {
                is AuthState.Failed -> StatusMessage(
                    text = authState.message,
                    color = MaterialTheme.colorScheme.error
                )
                is AuthState.Cancelled -> StatusMessage(
                    text = "Скасовано. Натисніть кнопку, щоб спробувати знову.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                is AuthState.Unavailable -> StatusMessage(
                    text = "Біометрія недоступна на цьому пристрої.",
                    color = MaterialTheme.colorScheme.error
                )
                else -> {}
            }
        }
    }
}

@Composable
private fun StatusMessage(text: String, color: androidx.compose.ui.graphics.Color) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = color,
        textAlign = TextAlign.Center
    )
}
