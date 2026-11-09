package com.punjabibeats.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.punjabibeats.app.domain.model.JamReaction
import kotlin.math.roundToInt
import kotlin.random.Random
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

private class FloatingReaction(val reaction: JamReaction, val xFraction: Float) {
    val progress = Animatable(0f)
}

/**
 * Emoji reactions that float from the bottom of the screen to the top and fade
 * out, live-stream style. Each reaction animates on its own coroutine so rapid
 * taps stack instead of cancelling each other.
 */
@Composable
fun ReactionsOverlay(
    reactions: StateFlow<JamReaction?>,
    modifier: Modifier = Modifier,
) {
    val latest by reactions.collectAsStateWithLifecycle()
    val floating = remember { mutableStateListOf<FloatingReaction>() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(latest) {
        val reaction = latest ?: return@LaunchedEffect
        val item = FloatingReaction(reaction, xFraction = 0.12f + Random.nextFloat() * 0.7f)
        floating.add(item)
        scope.launch {
            item.progress.animateTo(1f, tween(durationMillis = 2600))
            floating.remove(item)
        }
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val heightPx = constraints.maxHeight.toFloat()
        val widthPx = constraints.maxWidth.toFloat()
        floating.forEach { item ->
            val progress = item.progress.value
            if (progress <= 0f) return@forEach
            Text(
                text = item.reaction.emoji,
                fontSize = 30.sp,
                modifier = Modifier
                    .graphicsLayer {
                        alpha = (1f - progress).coerceIn(0f, 1f)
                        scaleX = 0.7f + 0.5f * progress
                        scaleY = 0.7f + 0.5f * progress
                    }
                    .offset {
                        IntOffset(
                            (item.xFraction * widthPx).roundToInt(),
                            (heightPx - progress * (heightPx + 120f)).roundToInt(),
                        )
                    },
            )
        }
    }
}
