package com.securechat.data.remote.signaling;

import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Provider;
import dagger.internal.Providers;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import okhttp3.OkHttpClient;

@ScopeMetadata("javax.inject.Singleton")
@QualifierMetadata
@DaggerGenerated
@Generated(
    value = "dagger.internal.codegen.ComponentProcessor",
    comments = "https://dagger.dev"
)
@SuppressWarnings({
    "unchecked",
    "rawtypes",
    "KotlinInternal",
    "KotlinInternalInJava",
    "cast",
    "deprecation",
    "nullness:initialization.field.uninitialized"
})
public final class SignalingWebSocketClient_Factory implements Factory<SignalingWebSocketClient> {
  private final Provider<OkHttpClient> okHttpClientProvider;

  private final Provider<String> wsBaseUrlProvider;

  public SignalingWebSocketClient_Factory(Provider<OkHttpClient> okHttpClientProvider,
      Provider<String> wsBaseUrlProvider) {
    this.okHttpClientProvider = okHttpClientProvider;
    this.wsBaseUrlProvider = wsBaseUrlProvider;
  }

  @Override
  public SignalingWebSocketClient get() {
    return newInstance(okHttpClientProvider.get(), wsBaseUrlProvider.get());
  }

  public static SignalingWebSocketClient_Factory create(
      javax.inject.Provider<OkHttpClient> okHttpClientProvider,
      javax.inject.Provider<String> wsBaseUrlProvider) {
    return new SignalingWebSocketClient_Factory(Providers.asDaggerProvider(okHttpClientProvider), Providers.asDaggerProvider(wsBaseUrlProvider));
  }

  public static SignalingWebSocketClient_Factory create(Provider<OkHttpClient> okHttpClientProvider,
      Provider<String> wsBaseUrlProvider) {
    return new SignalingWebSocketClient_Factory(okHttpClientProvider, wsBaseUrlProvider);
  }

  public static SignalingWebSocketClient newInstance(OkHttpClient okHttpClient, String wsBaseUrl) {
    return new SignalingWebSocketClient(okHttpClient, wsBaseUrl);
  }
}
