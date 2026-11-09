package com.punjabibeats.app.core.di

import android.content.Context
import com.punjabibeats.app.BuildConfig
import com.punjabibeats.app.core.util.NetworkMonitor
import com.punjabibeats.app.core.util.PbJson
import com.punjabibeats.app.data.remote.OfflineCacheInterceptor
import com.punjabibeats.app.data.remote.api.AudiusApi
import com.punjabibeats.app.data.remote.api.BackendApi
import com.punjabibeats.app.data.remote.api.ITunesApi
import com.punjabibeats.app.data.remote.api.JioSaavnApi
import com.punjabibeats.app.data.remote.api.LrcLibApi
import com.punjabibeats.app.data.remote.api.PipedApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.io.File
import java.util.concurrent.TimeUnit
import javax.inject.Singleton
import okhttp3.Cache
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(
        @ApplicationContext context: Context,
        networkMonitor: NetworkMonitor,
    ): OkHttpClient = OkHttpClient.Builder()
        .cache(Cache(File(context.cacheDir, "http_cache"), 50L * 1024L * 1024L))
        .addInterceptor(OfflineCacheInterceptor(networkMonitor))
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .pingInterval(20, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    private fun retrofit(baseUrl: String, okHttpClient: OkHttpClient): Retrofit = Retrofit.Builder()
        .baseUrl(baseUrl)
        .client(okHttpClient)
        .addConverterFactory(PbJson.json.asConverterFactory("application/json".toMediaType()))
        .build()

    @Provides
    @Singleton
    fun provideBackendApi(okHttpClient: OkHttpClient): BackendApi =
        retrofit(BuildConfig.BASE_URL, okHttpClient).create(BackendApi::class.java)

    @Provides
    @Singleton
    fun provideJioSaavnApi(okHttpClient: OkHttpClient): JioSaavnApi =
        retrofit(BuildConfig.JIOSAAVN_BASE_URL, okHttpClient).create(JioSaavnApi::class.java)

    @Provides
    @Singleton
    fun provideLrcLibApi(okHttpClient: OkHttpClient): LrcLibApi =
        retrofit(BuildConfig.LRCLIB_BASE_URL, okHttpClient).create(LrcLibApi::class.java)

    @Provides
    @Singleton
    fun provideAudiusApi(okHttpClient: OkHttpClient): AudiusApi =
        retrofit("https://discoveryprovider.audius.co/", okHttpClient).create(AudiusApi::class.java)

    @Provides
    @Singleton
    fun provideITunesApi(okHttpClient: OkHttpClient): ITunesApi =
        retrofit("https://itunes.apple.com/", okHttpClient).create(ITunesApi::class.java)

    @Provides
    @Singleton
    fun providePipedApi(okHttpClient: OkHttpClient): PipedApi =
        retrofit("https://pipedapi.kavin.rocks/", okHttpClient).create(PipedApi::class.java)
}
