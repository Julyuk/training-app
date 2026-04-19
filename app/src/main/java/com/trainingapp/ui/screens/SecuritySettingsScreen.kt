package com.trainingapp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.trainingapp.data.biometric.BiometricType
import com.trainingapp.ui.viewmodel.SecuritySettingsViewModel

/**
 * Security settings screen.
 *
 * Lets the user:
 *  1. Enable / disable biometric authentication with a toggle.
 *  2. See which sensor type is present on the device.
 *  3. Choose the inactivity timeout before the app auto-locks.
 *
 * All changes are written to [SecurityPreferences] immediately through the
 * ViewModel, so they persist across app restarts.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecuritySettingsScreen(
    viewModel: SecuritySettingsViewModel,
    onBack: () -> Unit
) {
    val isBiometricEnabled by viewModel.isBiometricEnabled.collectAsStateWithLifecycle()
    val autoLockTimeout by viewModel.autoLockTimeout.collectAsStateWithLifecycle()
    val biometricType = viewModel.biometricType

    val timeoutOptions = listOf(15, 30, 60, 120, 300)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Безпека") },
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
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {

            // ── Sensor info card ──────────────────────────────────────────────
            SectionHeader(text = "Датчик на пристрої")

            OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Fingerprint,
                        contentDescription = null,
                        tint = if (biometricType != BiometricType.NONE)
                            MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.outline
                    )
                    Column {
                        Text(
                            text = "${biometricType.icon()} ${biometricType.displayName()}",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = if (biometricType != BiometricType.NONE)
                                "Датчик знайдено і готовий до використання"
                            else
                                "Біометричний датчик відсутній або не налаштований",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // ── Biometric toggle ──────────────────────────────────────────────
            SectionHeader(text = "Автентифікація")

            OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.Lock,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(end = 12.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Біометричний вхід",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = "Використовувати ${biometricType.displayName()} для входу в застосунок",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = isBiometricEnabled,
                        onCheckedChange = { viewModel.setBiometricEnabled(it) },
                        enabled = biometricType != BiometricType.NONE
                    )
                }
            }

            // ── Auto-lock timeout ─────────────────────────────────────────────
            if (isBiometricEnabled) {
                Spacer(modifier = Modifier.height(8.dp))
                SectionHeader(text = "Автоблокування")

                OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Timer,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Заблокувати після: ${formatTimeout(autoLockTimeout)}",
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        timeoutOptions.forEach { seconds ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                RadioButton(
                                    selected = autoLockTimeout == seconds,
                                    onClick = { viewModel.setAutoLockTimeout(seconds) }
                                )
                                Text(
                                    text = formatTimeout(seconds),
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(start = 4.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (biometricType == BiometricType.NONE) {
                Text(
                    text = "Щоб увімкнути біометрію, спершу налаштуйте відбитки пальців або Face ID в налаштуваннях системи.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(vertical = 4.dp)
    )
}

private fun formatTimeout(seconds: Int): String = when {
    seconds < 60   -> "$seconds сек"
    seconds < 3600 -> "${seconds / 60} хв"
    else           -> "${seconds / 3600} год"
}
