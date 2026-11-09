package com.punjabibeats.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.punjabibeats.app.ui.theme.GlassFillEnd
import com.punjabibeats.app.ui.theme.GlassFillStart
import com.punjabibeats.app.ui.theme.GlassStrokeEnd
import com.punjabibeats.app.ui.theme.GlassStrokeStart

/**
 * Frosted-glass card: translucent gradient fill, hairline gradient stroke.
 * Content flows in a [Column].
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 20.dp,
    contentPadding: Dp = 16.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(cornerRadius)
    Column(
        modifier = modifier
            .clip(shape)
            .background(Brush.linearGradient(listOf(GlassFillStart, GlassFillEnd)))
            .border(1.dp, Brush.linearGradient(listOf(GlassStrokeStart, GlassStrokeEnd)), shape)
            .padding(contentPadding),
        content = content,
    )
}
