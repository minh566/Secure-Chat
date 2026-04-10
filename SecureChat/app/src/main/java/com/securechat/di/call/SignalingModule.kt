package com.securechat.di.call
import com.securechat.data.remote.signaling.SignalingApiClient
import com.securechat.data.remote.signaling.SignalingWebSocketClient
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SignalingModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(8, TimeUnit.SECONDS)
            .readTimeout(0, TimeUnit.MILLISECONDS)
            .build()
    }

    @Provides
    @Named("signalingHttpUrl")
    fun provideSignalingHttpUrl(): String = "http://10.0.2.2:8081"

    @Provides
    @Named("signalingWsUrl")
    fun provideSignalingWsUrl(): String = "ws://10.0.2.2:8081/ws"

    @Provides
    @Singleton
    fun provideSignalingApiClient(
        okHttpClient: OkHttpClient,
        @Named("signalingHttpUrl") baseHttpUrl: String
    ): SignalingApiClient = SignalingApiClient(okHttpClient, baseHttpUrl)

    @Provides
    @Singleton
    fun provideSignalingWebSocketClient(
        okHttpClient: OkHttpClient,
        @Named("signalingWsUrl") wsBaseUrl: String
    ): SignalingWebSocketClient = SignalingWebSocketClient(okHttpClient, wsBaseUrl)
}

