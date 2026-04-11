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

internal data class SignalingEndpoints(
    val httpUrl: String,
    val wsUrl: String,
    val sfuWsUrl: String
)

@Module
@InstallIn(SingletonComponent::class)
object SignalingModule {

    private fun endpoints(): SignalingEndpoints {
        return SignalingUrlConfigValidator.validate(
            httpUrl = BuildConfig.SIGNALING_HTTP_URL,
            wsUrl = BuildConfig.SIGNALING_WS_URL,
            sfuWsUrl = BuildConfig.SFU_WS_URL,
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
    fun provideSignalingHttpUrl(): String = endpoints().httpUrl

    @Provides
    @Named("signalingWsUrl")
    fun provideSignalingWsUrl(): String = endpoints().wsUrl

    @Provides
    @Named("sfuWsUrl")
    fun provideSfuWsUrl(): String = endpoints().sfuWsUrl

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
    fun validate(httpUrl: String, wsUrl: String, sfuWsUrl: String, flavor: String): SignalingEndpoints {
        val normalizedHttp = httpUrl.trim()
        val normalizedWs = wsUrl.trim()
        val normalizedSfuWs = sfuWsUrl.trim()

        require(normalizedHttp.isNotBlank()) { "SIGNALING_HTTP_URL must not be blank" }
        require(normalizedWs.isNotBlank()) { "SIGNALING_WS_URL must not be blank" }
        require(normalizedSfuWs.isNotBlank()) { "SFU_WS_URL must not be blank" }

        if (flavor.equals("prod", ignoreCase = true)) {
            require(normalizedHttp.startsWith("https://")) {
                "Production signaling HTTP URL must use https"
            }
            require(normalizedWs.startsWith("wss://")) {
                "Production signaling WS URL must use wss"
            }
            require(normalizedSfuWs.startsWith("wss://")) {
                "Production SFU URL must use wss"
            }
        }

        return SignalingEndpoints(
            httpUrl = normalizedHttp,
            wsUrl = normalizedWs,
            sfuWsUrl = normalizedSfuWs
        )
    }
}

