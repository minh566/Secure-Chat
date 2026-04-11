package com.securechat.di.call;

import com.securechat.data.remote.signaling.SignalingApiClient;
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
public final class SignalingModule_ProvideSignalingApiClientFactory implements Factory<SignalingApiClient> {
  private final Provider<OkHttpClient> okHttpClientProvider;

  private final Provider<String> baseHttpUrlProvider;

  public SignalingModule_ProvideSignalingApiClientFactory(
      Provider<OkHttpClient> okHttpClientProvider, Provider<String> baseHttpUrlProvider) {
    this.okHttpClientProvider = okHttpClientProvider;
    this.baseHttpUrlProvider = baseHttpUrlProvider;
  }

  @Override
  public SignalingApiClient get() {
    return provideSignalingApiClient(okHttpClientProvider.get(), baseHttpUrlProvider.get());
  }

  public static SignalingModule_ProvideSignalingApiClientFactory create(
      javax.inject.Provider<OkHttpClient> okHttpClientProvider,
      javax.inject.Provider<String> baseHttpUrlProvider) {
    return new SignalingModule_ProvideSignalingApiClientFactory(Providers.asDaggerProvider(okHttpClientProvider), Providers.asDaggerProvider(baseHttpUrlProvider));
  }

  public static SignalingModule_ProvideSignalingApiClientFactory create(
      Provider<OkHttpClient> okHttpClientProvider, Provider<String> baseHttpUrlProvider) {
    return new SignalingModule_ProvideSignalingApiClientFactory(okHttpClientProvider, baseHttpUrlProvider);
  }

  public static SignalingApiClient provideSignalingApiClient(OkHttpClient okHttpClient,
      String baseHttpUrl) {
    return Preconditions.checkNotNullFromProvides(SignalingModule.INSTANCE.provideSignalingApiClient(okHttpClient, baseHttpUrl));
  }
}
