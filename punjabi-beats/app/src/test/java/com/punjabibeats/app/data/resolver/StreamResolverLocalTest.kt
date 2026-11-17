package com.punjabibeats.app.data.resolver

import com.punjabibeats.app.domain.model.MusicSource
import com.punjabibeats.app.domain.model.Song
import com.punjabibeats.app.domain.model.StreamResolutionException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class StreamResolverLocalTest {

    @Suppress("UNCHECKED_CAST")
    private fun <T> createUnsafeInstance(clazz: Class<T>): T {
        val unsafeClass = Class.forName("sun.misc.Unsafe")
        val theUnsafeField = unsafeClass.getDeclaredField("theUnsafe").apply { isAccessible = true }
        val unsafe = theUnsafeField.get(null)
        val allocateInstanceMethod = unsafeClass.getMethod("allocateInstance", Class::class.java)
        return allocateInstanceMethod.invoke(unsafe, clazz) as T
    }

    private fun createStreamResolver(): StreamResolver {
        return StreamResolver(
            zenMusicResolver = createUnsafeInstance(ZenMusicResolver::class.java),
            jioSaavnResolver = createUnsafeInstance(JioSaavnResolver::class.java),
            youTubeStreamResolver = createUnsafeInstance(YouTubeStreamResolver::class.java),
            innerTubeResolver = createUnsafeInstance(com.punjabibeats.app.innertube.InnerTubeResolver::class.java),
            audiusResolver = createUnsafeInstance(AudiusResolver::class.java),
            iTunesResolver = createUnsafeInstance(ITunesResolver::class.java),
        )
    }

    @Test
    fun testLocalTrackWithValidStreamUrlReturnsImmediately() = runBlocking {
        val resolver = createStreamResolver()
        val localSong = Song(
            id = "local_123",
            title = "My Local Punjabi Track",
            artist = "Local Artist",
            streamUrl = "content://media/external/audio/media/123",
            source = MusicSource.LOCAL,
        )

        val result = resolver.resolve(localSong)
        assertEquals("content://media/external/audio/media/123", result.streamUrl)
    }

    @Test(expected = StreamResolutionException::class)
    fun testLocalTrackWithMissingStreamUrlFailsImmediatelyWithoutScraping(): Unit = runBlocking {
        val resolver = createStreamResolver()
        val missingSong = Song(
            id = "local_missing",
            title = "Missing Track",
            artist = "Local Artist",
            streamUrl = null,
            source = MusicSource.LOCAL,
        )

        resolver.resolve(missingSong)
    }
}
