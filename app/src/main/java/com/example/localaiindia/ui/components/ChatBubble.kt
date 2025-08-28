package com.example.localaiindia.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.localaiindia.model.ChatMessage
import com.example.localaiindia.ui.theme.*

@Composable
fun ChatBubble(
    message: ChatMessage,
    isDarkTheme: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        if (message.isFromUser) {
            UserChatBubble(
                message = message,
                isDarkTheme = isDarkTheme
            )
        } else {
            AiChatBubble(
                message = message,
                isDarkTheme = isDarkTheme,
                onCopy = { copyToClipboard(context, message.text) }
            )
        }
    }
}

private fun copyToClipboard(context: Context, text: String) {
    val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clipData = ClipData.newPlainText("AI Response", text)
    clipboardManager.setPrimaryClip(clipData)
    Toast.makeText(context, "Message copied to clipboard", Toast.LENGTH_SHORT).show()
}

@Composable
private fun UserChatBubble(
    message: ChatMessage,
    isDarkTheme: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End
    ) {
        Card(
            modifier = Modifier.widthIn(max = 280.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isDarkTheme) PrimaryDark else Primary
            )
        ) {
            Text(
                text = message.text,
                modifier = Modifier.padding(16.dp),
                color = Color.White,
                fontSize = 16.sp,
                lineHeight = 20.sp
            )
        }
    }
}

@Composable
private fun AiChatBubble(
    message: ChatMessage,
    isDarkTheme: Boolean,
    onCopy: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Card(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .align(Alignment.Start),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isDarkTheme) SurfaceDark else Surface
            )
        ) {
            if (message.isTyping) {
                TypingIndicator(isDarkTheme = isDarkTheme)
            } else {
                Column {
                    Text(
                        text = message.text,
                        modifier = Modifier.padding(16.dp),
                        color = if (isDarkTheme) OnSurfaceDark else OnSurface,
                        fontSize = 16.sp,
                        lineHeight = 20.sp
                    )

                    // Copy button
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, bottom = 8.dp)
                    ) {
                        TextButton(
                            onClick = onCopy,
                            modifier = Modifier.height(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.ContentCopy,
                                contentDescription = "Copy message",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Copy",
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TypingIndicator(isDarkTheme: Boolean) {
    val infiniteTransition = rememberInfiniteTransition()
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800)
        )
    )

    Row(
        modifier = Modifier.padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(3) { index ->
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .alpha(if (index == 0) alpha else if (index == 1) 1f - alpha else alpha)
                    .background(
                        color = if (isDarkTheme) OnSurfaceDark.copy(alpha = 0.6f) else OnSurface.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(4.dp)
                    )
            )
        }
    }
}