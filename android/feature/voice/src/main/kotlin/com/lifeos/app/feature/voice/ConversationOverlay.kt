package com.lifeos.app.feature.voice

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

/**
 * A transcript, not a message thread — deliberately not chat bubbles (see
 * the 2026-08-12 redesign audit's own finding: the old bubble UI was "a
 * chatbot attached to a voice button"). Juno's own words carry
 * typographic weight — the serif "speaking" voice; the user's words are a
 * quiet, muted record underneath, since they already know what they said.
 */
@Composable
fun ConversationOverlay(
    visible: Boolean,
    presence: PresenceState,
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
                    .background(Color(0xFF1B1611))
                    // Swallow taps so they don't fall through to the scrim's dismiss.
                    .clickable(onClick = {})
                    .padding(top = 12.dp, start = 24.dp, end = 24.dp, bottom = 28.dp),
            ) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .size(width = 36.dp, height = 4.dp)
                        .background(Color.White.copy(alpha = 0.25f), RoundedCornerShape(2.dp)),
                )

                Spacer(modifier = Modifier.height(20.dp))

                Box(modifier = Modifier.align(Alignment.CenterHorizontally)) {
                    JunoPresence(state = presence, onClick = onOrbClick, orbSize = 120.dp)
                }

                Spacer(modifier = Modifier.height(20.dp))

                // On the very first tap, conversation is empty and the panel
                // used to just be the Presence sitting in an otherwise-blank
                // dark box — easy to mistake for broken rather than
                // "waiting for you to say something."
                if (conversation.isEmpty()) {
                    Text(
                        text = if (presence == PresenceState.LISTENING) "Listening…" else "Tap the circle and talk to me",
                        color = Color.White.copy(alpha = 0.7f),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                    )
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.weight(1f, fill = false),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        items(conversation) { turn -> ConversationLine(turn) }
                    }
                }
            }
        }
    }
}

@Composable
private fun ConversationLine(turn: ConversationTurn) {
    if (turn.fromUser) {
        Text(
            text = turn.text,
            color = Color.White.copy(alpha = 0.55f),
            fontSize = 14.sp,
            modifier = Modifier.padding(start = 16.dp),
        )
    } else {
        Text(
            text = turn.text,
            color = Color.White,
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.Normal,
            fontSize = 19.sp,
            lineHeight = 27.sp,
        )
    }
}
