package com.securechat.di.call;

import com.securechat.data.remote.signaling.SignalingWebSocketClient;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
import dagger.internal.Provider;
import dagger.internal.Providers;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import okhttp3.OkHttpClient;

@ScopeMetadata("javax.inject.Singleton")
@QualifierMetadata("javax.inject.Named")
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
public final class SignalingModule_ProvideSignalingWebSocketClientFactory implements Factory<SignalingWebSocketClient> {
  private final Provider<OkHttpClient> okHttpClientProvider;

  private final Provider<String> wsBaseUrlProvider;

  public SignalingModule_ProvideSignalingWebSocketClientFactory(
      Provider<OkHttpClient> okHttpClientProvider, Provider<String> wsBaseUrlProvider) {
    this.okHttpClientProvider = okHttpClientProvider;
    this.wsBaseUrlProvider = wsBaseUrlProvider;
  }

  @Override
  public SignalingWebSocketClient get() {
    return provideSignalingWebSocketClient(okHttpClientProvider.get(), wsBaseUrlProvider.get());
  }

  public static SignalingModule_ProvideSignalingWebSocketClientFactory create(
      javax.inject.Provider<OkHttpClient> okHttpClientProvider,
      javax.inject.Provider<String> wsBaseUrlProvider) {
    return new SignalingModule_ProvideSignalingWebSocketClientFactory(Providers.asDaggerProvider(okHttpClientProvider), Providers.asDaggerProvider(wsBaseUrlProvider));
  }

  public static SignalingModule_ProvideSignalingWebSocketClientFactory create(
      Provider<OkHttpClient> okHttpClientProvider, Provider<String> wsBaseUrlProvider) {
    return new SignalingModule_ProvideSignalingWebSocketClientFactory(okHttpClientProvider, wsBaseUrlProvider);
  }

  public static SignalingWebSocketClient provideSignalingWebSocketClient(OkHttpClient okHttpClient,
      String wsBaseUrl) {
    return Preconditions.checkNotNullFromProvides(SignalingModule.INSTANCE.provideSignalingWebSocketClient(okHttpClient, wsBaseUrl));
  }
}
