package com.punjabibeats.app.core.di

import android.content.Context
import com.punjabibeats.app.data.local.db.DownloadDao
import com.punjabibeats.app.data.local.db.HistoryDao
import com.punjabibeats.app.data.local.db.JamDao
import com.punjabibeats.app.data.local.db.LibraryDao
import com.punjabibeats.app.data.local.db.PunjabiBeatsDatabase
import com.punjabibeats.app.data.local.db.SearchDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Qualifier
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ApplicationScope

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): PunjabiBeatsDatabase =
        PunjabiBeatsDatabase.build(context)

    @Provides
    fun provideLibraryDao(database: PunjabiBeatsDatabase): LibraryDao = database.libraryDao()

    @Provides
    fun provideSearchDao(database: PunjabiBeatsDatabase): SearchDao = database.searchDao()

    @Provides
    fun provideHistoryDao(database: PunjabiBeatsDatabase): HistoryDao = database.historyDao()

    @Provides
    fun provideDownloadDao(database: PunjabiBeatsDatabase): DownloadDao = database.downloadDao()

    @Provides
    fun provideJamDao(database: PunjabiBeatsDatabase): JamDao = database.jamDao()

    @Provides
    @Singleton
    @ApplicationScope
    fun provideApplicationScope(): CoroutineScope =
        CoroutineScope(SupervisorJob() + Dispatchers.Default)
}
