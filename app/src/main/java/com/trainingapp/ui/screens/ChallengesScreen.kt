package com.trainingapp.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Group
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.trainingapp.data.model.Challenge
import com.trainingapp.data.websocket.WebSocketMessage
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Shows a list of community fitness challenges and the latest real-time
 * update received from the server over the WebSocket channel.
 *
 * The [lastUpdate] banner appears at the top of the screen whenever a
 * [com.trainingapp.data.websocket.MessageType.CHALLENGE_UPDATE] event arrives,
 * so the screen reacts to the server without any manual refresh.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChallengesScreen(
    challenges: List<Challenge>,
    lastUpdate: WebSocketMessage?,
    onToggleJoin: (Int) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Виклики") },
                navigationIcon = {
                    Icon(
                        imageVector = Icons.Filled.EmojiEvents,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .padding(start = 12.dp)
                            .size(28.dp)
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {

            // ── Real-time update banner (appears when WS delivers a challenge event)
            AnimatedVisibility(
                visible = lastUpdate != null,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                lastUpdate?.let { msg ->
                    Surface(
                        color = MaterialTheme.colorScheme.tertiaryContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("🎯", style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = msg.title,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = msg.body,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }
            }

            // ── Joined count summary
            val joinedCount = challenges.count { it.isJoined }
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Ти берешь участь у $joinedCount з ${challenges.size} викликів",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                )
            }

            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(challenges, key = { it.id }) { challenge ->
                    ChallengeCard(challenge = challenge, onToggleJoin = onToggleJoin)
                }
            }
        }
    }
}

// ── Sub-composables ───────────────────────────────────────────────────────────

private val SHORT_DATE_FMT = DateTimeFormatter.ofPattern("d MMM", Locale("uk"))

@Composable
private fun ChallengeCard(challenge: Challenge, onToggleJoin: (Int) -> Unit) {
    val today = LocalDate.now()
    val daysLeft = (challenge.endDate.toEpochDay() - today.toEpochDay()).coerceAtLeast(0)
    val isActive = !today.isBefore(challenge.startDate) && !today.isAfter(challenge.endDate)
    val isExpired = today.isAfter(challenge.endDate)

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = when {
                isExpired -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                challenge.isJoined -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                else -> MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            // Title row
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (challenge.isJoined) {
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = "Ти берешь участь",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                }
                Text(
                    text = challenge.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(4.dp))
            Text(
                text = challenge.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(6.dp))

            // Date range row
            val dateRangeText = "${challenge.startDate.format(SHORT_DATE_FMT)} – ${challenge.endDate.format(SHORT_DATE_FMT)}"
            val statusText = when {
                isExpired -> "Завершено"
                isActive  -> "⏳ $daysLeft дн. залишилось"
                else      -> "Починається ${challenge.startDate.format(SHORT_DATE_FMT)}"
            }
            val statusColor = when {
                isExpired -> MaterialTheme.colorScheme.onSurfaceVariant
                daysLeft <= 2 -> Color(0xFFE53935)
                else -> MaterialTheme.colorScheme.onSurfaceVariant
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                MetaChip("📅 $dateRangeText")
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.labelSmall,
                    color = statusColor
                )
            }

            // Progress (only for enrolled + active challenges)
            if (challenge.isJoined && !isExpired) {
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Прогрес",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${challenge.progressPct}%",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Spacer(Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = { challenge.progressPct / 100f },
                    modifier = Modifier.fillMaxWidth(),
                    color = if (challenge.progressPct >= 100)
                        Color(0xFF4CAF50)
                    else
                        MaterialTheme.colorScheme.primary
                )
            } else if (challenge.isJoined && isExpired) {
                Spacer(Modifier.height(10.dp))
                Text(
                    text = if (challenge.progressPct >= 100) "✅ Виклик виконано!" else "Виклик завершено — ${challenge.progressPct}% виконано",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (challenge.progressPct >= 100) Color(0xFF4CAF50)
                            else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.height(10.dp))

            // Bottom row: participants + action button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Group,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${challenge.participants} учасників",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (!isExpired) {
                    if (challenge.isJoined) {
                        OutlinedButton(
                            onClick = { onToggleJoin(challenge.id) },
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Text("Вийти", style = MaterialTheme.typography.labelSmall)
                        }
                    } else {
                        Button(onClick = { onToggleJoin(challenge.id) }) {
                            Text("Приєднатись", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MetaChip(text: String) {
    Surface(
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}
