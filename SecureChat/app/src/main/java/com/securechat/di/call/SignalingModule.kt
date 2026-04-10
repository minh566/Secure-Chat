
package com.securechat.di.call

import com.securechat.BuildConfig
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

    private fun endpoints(): Pair<String, String> {
        return SignalingUrlConfigValidator.validate(
            httpUrl = BuildConfig.SIGNALING_HTTP_URL,
            wsUrl = BuildConfig.SIGNALING_WS_URL,
            flavor = BuildConfig.FLAVOR
        )
    }

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
    fun provideSignalingHttpUrl(): String = endpoints().first

    @Provides
    @Named("signalingWsUrl")
    fun provideSignalingWsUrl(): String = endpoints().second

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

internal object SignalingUrlConfigValidator {
    fun validate(httpUrl: String, wsUrl: String, flavor: String): Pair<String, String> {
        val normalizedHttp = httpUrl.trim()
        val normalizedWs = wsUrl.trim()

        require(normalizedHttp.isNotBlank()) { "SIGNALING_HTTP_URL must not be blank" }
        require(normalizedWs.isNotBlank()) { "SIGNALING_WS_URL must not be blank" }

        if (flavor.equals("prod", ignoreCase = true)) {
            require(normalizedHttp.startsWith("https://")) {
                "Production signaling HTTP URL must use https"
            }
            require(normalizedWs.startsWith("wss://")) {
                "Production signaling WS URL must use wss"
            }
        }

        return normalizedHttp to normalizedWs
    }
}

