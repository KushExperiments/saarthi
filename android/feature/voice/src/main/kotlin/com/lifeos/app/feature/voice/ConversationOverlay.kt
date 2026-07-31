package com.lifeos.app.feature.voice

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.lifeos.app.core.designsystem.LifeOSColors

/**
 * The "black box" conversation view — opens over whatever the user was
 * doing (tap-to-talk today, wake-word later) the same way Google Assistant
 * opens over the current app, rather than navigating away to a full screen.
 */
@Composable
fun ConversationOverlay(
    visible: Boolean,
    listening: Boolean,
    conversation: List<ConversationTurn>,
    onOrbClick: () -> Unit,
    onDismiss: () -> Unit,
) {
    if (!visible) return

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.55f))
                .clickable(onClick = onDismiss),
        ) {
            val listState = rememberLazyListState()
            LaunchedEffect(conversation.size) {
                if (conversation.isNotEmpty()) listState.animateScrollToItem(conversation.size - 1)
            }

            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .heightIn(min = 320.dp, max = 560.dp)
                    .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                    .background(Color(0xFF15131C))
                    // Swallow taps so they don't fall through to the scrim's dismiss.
                    .clickable(onClick = {})
                    .padding(top = 12.dp, start = 20.dp, end = 20.dp, bottom = 28.dp),
            ) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .size(width = 36.dp, height = 4.dp)
                        .background(Color.White.copy(alpha = 0.25f), RoundedCornerShape(2.dp)),
                )

                Spacer(modifier = Modifier.height(20.dp))

                Box(modifier = Modifier.align(Alignment.CenterHorizontally)) {
                    VoiceOrb(listening = listening, onClick = onOrbClick, orbSize = 120.dp)
                }

                Spacer(modifier = Modifier.height(20.dp))

                LazyColumn(
                    state = listState,
                    modifier = Modifier.weight(1f, fill = false),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(conversation) { turn -> ConversationBubble(turn) }
                }
            }
        }
    }
}

@Composable
private fun ConversationBubble(turn: ConversationTurn) {
    val alignment = if (turn.fromUser) Alignment.CenterEnd else Alignment.CenterStart
    val bubbleColor = if (turn.fromUser) LifeOSColors.VoiceAccent.copy(alpha = 0.85f) else Color.White.copy(alpha = 0.08f)

    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = alignment) {
        Text(
            text = turn.text,
            color = Color.White,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier
                .widthIn(max = 280.dp)
                .background(bubbleColor, RoundedCornerShape(18.dp))
                .padding(horizontal = 16.dp, vertical = 10.dp),
        )
    }
}
