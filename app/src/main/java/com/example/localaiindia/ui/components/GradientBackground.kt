package com.example.localaiindia.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import com.example.localaiindia.ui.theme.*

@Composable
fun GradientBackground(
    isDarkTheme: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val backgroundColor = if (isDarkTheme) {
        Brush.verticalGradient(
            colors = listOf(
                BackgroundDark,
                BackgroundDark.copy(alpha = 0.95f),
                BackgroundDark.copy(alpha = 0.9f)
            )
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(
                Background,
                Background.copy(alpha = 0.95f),
                Background.copy(alpha = 0.9f)
            )
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundColor)
    ) {
        content()
    }
}