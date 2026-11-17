package com.punjabibeats.app.ui.components

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.punjabibeats.app.ui.theme.LocalPBTheme

/**
 * Full-bleed artwork backdrop with a heavy blur — the "blur placeholder" layer
 * behind the hero banner and the full player. On pre-Android-12 devices blur
 * is unavailable, so a plain dark surface is drawn instead.
 */
@Composable
fun BlurredBackdropImage(
    url: String?,
    modifier: Modifier = Modifier,
    blurRadius: Dp = 48.dp,
    scrimAlpha: Float = 0.55f,
) {
    val theme = LocalPBTheme.current
    Box(modifier = modifier.fillMaxSize()) {
        if (url != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(url)
                    .crossfade(400)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .blur(blurRadius),
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = scrimAlpha)),
            )
        } else {
            Box(modifier = Modifier.fillMaxSize().background(theme.background))
        }
    }
}
