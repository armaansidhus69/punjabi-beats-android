package com.punjabibeats.app.ui.screens.lyrics

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.graphics.Typeface
import androidx.core.content.FileProvider
import com.punjabibeats.app.domain.model.Song
import java.io.File
import java.io.FileOutputStream
import kotlin.math.min

/**
 * Renders selected lyric lines into a 1080×1350 share card (WhatsApp /
 * Instagram story format) drawn manually on a Canvas so it works headlessly
 * without pixel-copying the Compose tree.
 */
object LyricCardRenderer {

    private const val CARD_WIDTH = 1080
    private const val CARD_HEIGHT = 1350
    private const val PADDING = 96f

    fun render(context: Context, song: Song, lines: List<String>): Bitmap {
        val bitmap = Bitmap.createBitmap(CARD_WIDTH, CARD_HEIGHT, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val background = Paint().apply {
            shader = LinearGradient(
                0f, 0f, 0f, CARD_HEIGHT.toFloat(),
                intArrayOf(0xFFE50914.toInt(), 0xFF3A2E10.toInt(), 0xFF0A0A0F.toInt()),
                floatArrayOf(0.05f, 0.55f, 1f),
                Shader.TileMode.CLAMP,
            )
        }
        canvas.drawRect(0f, 0f, CARD_WIDTH.toFloat(), CARD_HEIGHT.toFloat(), background)

        val gold = 0xFFD4AF37.toInt()
        val white = 0xFFF5F5F7.toInt()

        val brandPaint = Paint().apply {
            color = gold
            textSize = 34f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            letterSpacing = 0.14f
            isAntiAlias = true
        }
        canvas.drawText("PUNJABI  BEATS", PADDING, 140f, brandPaint)

        val bodyPaint = Paint().apply {
            color = white
            textSize = 64f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            isAntiAlias = true
        }
        val maxLines = 8
        val bodyWidth = CARD_WIDTH - 2 * PADDING
        var y = 300f
        lines.take(maxLines).forEach { line ->
            val layout = android.text.StaticLayout.Builder
                .obtain(line, 0, line.length, android.text.TextPaint(bodyPaint), bodyWidth.toInt())
                .setAlignment(android.text.Layout.Alignment.ALIGN_CENTER)
                .setLineSpacing(14f, 1f)
                .build()
            y += 40f
            canvas.save()
            canvas.translate(PADDING, y)
            layout.draw(canvas)
            canvas.restore()
            y += layout.height
        }

        val metaPaint = Paint().apply {
            color = white
            textSize = 42f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            isAntiAlias = true
        }
        val meta = "${song.title} — ${song.artist}"
        val ellipsized = if (meta.length > 44) meta.take(41) + "…" else meta
        canvas.drawText(ellipsized, PADDING, (CARD_HEIGHT - 170f), metaPaint)

        val creditPaint = Paint().apply {
            color = gold
            textSize = 32f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            isAntiAlias = true
        }
        canvas.drawText("shared via Punjabi Beats", PADDING, (CARD_HEIGHT - 110f), creditPaint)

        return bitmap
    }

    fun share(context: Context, bitmap: Bitmap, onDone: () -> Unit = {}) {
        val dir = File(context.cacheDir, "share").apply { mkdirs() }
        val file = File(dir, "lyric_card_${System.currentTimeMillis()}.png")
        FileOutputStream(file).use { out -> bitmap.compress(Bitmap.CompressFormat.PNG, 95, out) }
        val uri = FileProvider.getUriForFile(context, "com.punjabibeats.app.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            clipData = android.content.ClipData.newUri(context.contentResolver, "Lyric Card", uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(intent, "Share lyric card").apply {
            clipData = android.content.ClipData.newUri(context.contentResolver, "Lyric Card", uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { context.startActivity(chooser) }
        onDone()
    }

    fun clampLineCount(lines: List<String>, max: Int = 8): List<String> = lines.take(min(lines.size, max))
}
