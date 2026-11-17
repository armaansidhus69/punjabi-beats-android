package com.punjabibeats.app.playback

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.TransferListener
import com.punjabibeats.app.data.security.EncryptedFileDataSource
import java.util.concurrent.CopyOnWriteArrayList

/** Routes `pbenc://` schemes to the AES/GCM offline DataSource, everything else upstream. */
@UnstableApi
class PbDataSourceFactory(
    private val context: Context,
    private val upstream: DataSource.Factory,
) : DataSource.Factory {

    override fun createDataSource(): DataSource = RoutingDataSource(context, upstream.createDataSource())

    companion object {
        @OptIn(UnstableApi::class)
        fun defaultFor(context: Context): PbDataSourceFactory {
            val http = DefaultHttpDataSource.Factory()
                .setAllowCrossProtocolRedirects(true)
                .setConnectTimeoutMs(15_000)
                .setReadTimeoutMs(15_000)
                .setUserAgent("PunjabiBeats/1.0")
            return PbDataSourceFactory(context, DefaultDataSource.Factory(context, http))
        }
    }
}

@UnstableApi
private class RoutingDataSource(
    private val context: Context,
    private val upstream: DataSource,
) : DataSource {

    private val transferListeners = CopyOnWriteArrayList<TransferListener>()
    private var activeDelegate: DataSource? = null

    override fun addTransferListener(transferListener: TransferListener) {
        transferListeners.add(transferListener)
        upstream.addTransferListener(transferListener)
    }

    override fun open(dataSpec: DataSpec): Long {
        close()
        val delegate = when {
            dataSpec.uri.scheme.equals("pbenc", ignoreCase = true) -> {
                EncryptedFileDataSource(context).also { source ->
                    transferListeners.forEach { source.addTransferListener(it) }
                }
            }
            // Zen engine stream/artwork uris are served by the engine's channel
            // transport — the real media location never leaves the engine.
            dataSpec.uri.scheme.equals("zen", ignoreCase = true) -> {
                val engine = com.zenmusic.ZenMusic.get() ?: com.zenmusic.ZenMusic.initialize(context)
                engine.dataSourceFactory.createDataSource().also { source ->
                    transferListeners.forEach { source.addTransferListener(it) }
                }
            }
            else -> upstream
        }
        activeDelegate = delegate
        return delegate.open(dataSpec)
    }

    override fun read(target: ByteArray, offset: Int, length: Int): Int =
        activeDelegate?.read(target, offset, length) ?: C.RESULT_END_OF_INPUT

    override fun getUri(): android.net.Uri? = activeDelegate?.uri

    override fun getResponseHeaders(): Map<String, List<String>> =
        activeDelegate?.responseHeaders ?: emptyMap()

    override fun close() {
        val delegate = activeDelegate
        activeDelegate = null
        runCatching { delegate?.close() }
    }
}
