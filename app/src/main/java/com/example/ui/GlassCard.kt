package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CardElevation
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp

/**
 * A reusable glass‑morphism style Card.
 *
 * It draws a semi‑transparent vertical gradient background and applies a blur to create a frosted‑glass
 * effect. The composable forwards all parameters to the underlying Material3 Card so it can be used as a drop‑in
 * replacement.
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = RoundedCornerShape(12.dp),
    border: androidx.compose.foundation.BorderStroke? = null,
    containerColor: Color = Color.Transparent,
    contentColor: Color = Color.Unspecified,
    elevation: CardElevation = CardDefaults.cardElevation(),
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.15f),
                        Color.White.copy(alpha = 0.05f)
                    )
                ),
                shape = shape
            )
            .blur(16.dp),
        shape = shape,
        border = border,
        colors = CardDefaults.cardColors(containerColor = containerColor, contentColor = contentColor),
        elevation = elevation,
        content = content
    )
}
