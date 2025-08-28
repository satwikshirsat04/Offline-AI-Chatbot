package com.example.localaiindia.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.example.localaiindia.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessageInput(
    message: String,
    onMessageChange: (String) -> Unit,
    onSendMessage: () -> Unit,
    isDarkTheme: Boolean,
    isEnabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = message,
            onValueChange = onMessageChange,
            enabled = isEnabled,
            modifier = Modifier
                .weight(1f)
                .height(56.dp),
            placeholder = {
                Text(
                    text = if (isEnabled) "Type your message..." else "AI model is initializing...",
                    color = if (isDarkTheme) OnSurfaceDark.copy(alpha = 0.6f) else OnSurface.copy(alpha = 0.6f)
                )
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = if (isDarkTheme) PrimaryDark else Primary,
                unfocusedBorderColor = if (isDarkTheme) OnSurfaceDark.copy(alpha = 0.3f) else OnSurface.copy(alpha = 0.3f),
                disabledBorderColor = if (isDarkTheme) OnSurfaceDark.copy(alpha = 0.2f) else OnSurface.copy(alpha = 0.2f),
                focusedTextColor = if (isDarkTheme) OnSurfaceDark else OnSurface,
                unfocusedTextColor = if (isDarkTheme) OnSurfaceDark else OnSurface,
                disabledTextColor = if (isDarkTheme) OnSurfaceDark.copy(alpha = 0.5f) else OnSurface.copy(alpha = 0.5f),
                cursorColor = if (isDarkTheme) PrimaryDark else Primary
            ),
            shape = RoundedCornerShape(28.dp),
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Sentences,
                imeAction = ImeAction.Send
            ),
            keyboardActions = KeyboardActions(
                onSend = {  // FIXED: Use onSend instead of onDone for ImeAction.Send
                    if (message.isNotBlank() && isEnabled) {
                        onSendMessage()
                    }
                }
            ),
            singleLine = true
        )

        Spacer(modifier = Modifier.width(8.dp))

        // Send button
        IconButton(
            onClick = {
                if (message.isNotBlank() && isEnabled) {
                    onSendMessage()
                }
            },
            enabled = isEnabled && message.isNotBlank(),
            modifier = Modifier
                .size(56.dp)
                .background(
                    color = if (isEnabled && message.isNotBlank()) {
                        if (isDarkTheme) PrimaryDark else Primary
                    } else {
                        if (isDarkTheme) OnSurfaceDark.copy(alpha = 0.3f) else OnSurface.copy(alpha = 0.3f)
                    },
                    shape = RoundedCornerShape(28.dp)
                )
        ) {
            Icon(
                imageVector = Icons.Default.Send,
                contentDescription = "Send message",
                tint = if (isEnabled && message.isNotBlank()) {
                    Color.White
                } else {
                    if (isDarkTheme) OnSurfaceDark.copy(alpha = 0.5f) else OnSurface.copy(alpha = 0.5f)
                },
                modifier = Modifier.size(24.dp)
            )
        }
    }
}