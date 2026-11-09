package com.punjabibeats.app.core.di

import com.punjabibeats.app.data.repository.AiRepositoryImpl
import com.punjabibeats.app.data.repository.DownloadRepositoryImpl
import com.punjabibeats.app.data.repository.LibraryRepositoryImpl
import com.punjabibeats.app.data.repository.LyricsRepositoryImpl
import com.punjabibeats.app.data.repository.PodcastRepositoryImpl
import com.punjabibeats.app.data.repository.SongRepositoryImpl
import com.punjabibeats.app.domain.repository.AiRepository
import com.punjabibeats.app.domain.repository.DownloadRepository
import com.punjabibeats.app.domain.repository.LibraryRepository
import com.punjabibeats.app.domain.repository.LyricsRepository
import com.punjabibeats.app.domain.repository.PodcastRepository
import com.punjabibeats.app.domain.repository.SongRepository
import com.punjabibeats.app.playback.MusicPlayerController
import com.punjabibeats.app.playback.PlayerControllerImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindSongRepository(impl: SongRepositoryImpl): SongRepository

    @Binds
    @Singleton
    abstract fun bindLibraryRepository(impl: LibraryRepositoryImpl): LibraryRepository

    @Binds
    @Singleton
    abstract fun bindLyricsRepository(impl: LyricsRepositoryImpl): LyricsRepository

    @Binds
    @Singleton
    abstract fun bindAiRepository(impl: AiRepositoryImpl): AiRepository

    @Binds
    @Singleton
    abstract fun bindPodcastRepository(impl: PodcastRepositoryImpl): PodcastRepository

    @Binds
    @Singleton
    abstract fun bindDownloadRepository(impl: DownloadRepositoryImpl): DownloadRepository

    @Binds
    @Singleton
    abstract fun bindPlayerController(impl: PlayerControllerImpl): MusicPlayerController
}
