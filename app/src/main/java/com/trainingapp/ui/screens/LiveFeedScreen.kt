package com.trainingapp.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import com.trainingapp.data.websocket.ConnectionState
import com.trainingapp.data.websocket.MessageType
import com.trainingapp.data.websocket.WebSocketMessage
import java.time.format.DateTimeFormatter

/**
 * Displays a scrollable real-time feed of events pushed by the server
 * over the WebSocket channel.
 *
 * The connection indicator in the top bar updates without any user action:
 *   - Green dot  → Connected
 *   - Wi-Fi-off  → Disconnected / Reconnecting
 *
 * New messages slide in at the top of the list.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiveFeedScreen(
    messages: List<WebSocketMessage>,
    connectionState: ConnectionState,
    onClearMessages: () -> Unit,
    onReconnect: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Живий канал") },
                navigationIcon = {
                    ConnectionIndicator(connectionState)
                },
                actions = {
                    if (messages.isNotEmpty()) {
                        IconButton(onClick = onClearMessages) {
                            Icon(Icons.Filled.Delete, contentDescription = "Очистити стрічку")
                        }
                    }
                    if (connectionState == ConnectionState.Disconnected) {
                        IconButton(onClick = onReconnect) {
                            Icon(Icons.Filled.Refresh, contentDescription = "Перепідключитись")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            // Connection status banner
            ConnectionBanner(connectionState)

            if (messages.isEmpty()) {
                EmptyFeedPlaceholder(connectionState, onReconnect)
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    itemsIndexed(
                        items = messages,
                        // Include the index so that if two messages ever share the
                        // same timestamp + title the key stays unique.
                        key = { index, msg -> "$index|${msg.timestamp}|${msg.title}" }
                    ) { _, msg ->
                        AnimatedVisibility(
                            visible = true,
                            enter = fadeIn() + slideInVertically(initialOffsetY = { -it / 2 })
                        ) {
                            FeedCard(msg)
                        }
                    }
                }
            }
        }
    }
}

// ── Sub-composables ───────────────────────────────────────────────────────────

@Composable
private fun ConnectionIndicator(state: ConnectionState) {
    Box(
        modifier = Modifier
            .padding(start = 12.dp)
            .size(36.dp),
        contentAlignment = Alignment.Center
    ) {
        when (state) {
            ConnectionState.Connected -> Icon(
                imageVector = Icons.Filled.FiberManualRecord,
                contentDescription = "Підключено",
                tint = Color(0xFF4CAF50),
                modifier = Modifier.size(16.dp)
            )
            else -> Icon(
                imageVector = Icons.Filled.WifiOff,
                contentDescription = "Немає зʼєднання",
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun ConnectionBanner(state: ConnectionState) {
    val (text, color) = when (state) {
        ConnectionState.Connected -> "Підключено до сервера" to Color(0xFF4CAF50)
        ConnectionState.Connecting -> "Підключення..." to Color(0xFFFFC107)
        ConnectionState.Reconnecting -> "Перепідключення..." to Color(0xFFFF9800)
        ConnectionState.Disconnected -> "Немає зʼєднання" to MaterialTheme.colorScheme.error
    }
    Surface(
        color = color.copy(alpha = 0.15f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = color,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
        )
    }
}

@Composable
private fun EmptyFeedPlaceholder(state: ConnectionState, onReconnect: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Notifications,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
            )
            Text(
                text = if (state == ConnectionState.Connected)
                    "Очікуємо нові події від сервера…"
                else
                    "Немає підключення до сервера",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (state == ConnectionState.Disconnected) {
                Button(onClick = onReconnect) {
                    Text("Підключитись")
                }
            }
        }
    }
}

@Composable
private fun FeedCard(message: WebSocketMessage) {
    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss")
    val (emoji, cardColor) = when (message.type) {
        MessageType.MOTIVATIONAL -> "💪" to MaterialTheme.colorScheme.primaryContainer
        MessageType.WORKOUT_COMPLETED -> "🏆" to MaterialTheme.colorScheme.secondaryContainer
        MessageType.CHALLENGE_UPDATE -> "🎯" to MaterialTheme.colorScheme.tertiaryContainer
        MessageType.NEW_WORKOUT_SUGGESTION -> "📋" to MaterialTheme.colorScheme.surfaceVariant
        MessageType.SYSTEM -> "ℹ️" to MaterialTheme.colorScheme.surfaceVariant
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = emoji, style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = message.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = message.timestamp.format(timeFormatter),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = message.body,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
