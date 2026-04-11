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
public final class SignalingApiClient_Factory implements Factory<SignalingApiClient> {
  private final Provider<OkHttpClient> okHttpClientProvider;

  private final Provider<String> baseHttpUrlProvider;

  public SignalingApiClient_Factory(Provider<OkHttpClient> okHttpClientProvider,
      Provider<String> baseHttpUrlProvider) {
    this.okHttpClientProvider = okHttpClientProvider;
    this.baseHttpUrlProvider = baseHttpUrlProvider;
  }

  @Override
  public SignalingApiClient get() {
    return newInstance(okHttpClientProvider.get(), baseHttpUrlProvider.get());
  }

  public static SignalingApiClient_Factory create(
      javax.inject.Provider<OkHttpClient> okHttpClientProvider,
      javax.inject.Provider<String> baseHttpUrlProvider) {
    return new SignalingApiClient_Factory(Providers.asDaggerProvider(okHttpClientProvider), Providers.asDaggerProvider(baseHttpUrlProvider));
  }

  public static SignalingApiClient_Factory create(Provider<OkHttpClient> okHttpClientProvider,
      Provider<String> baseHttpUrlProvider) {
    return new SignalingApiClient_Factory(okHttpClientProvider, baseHttpUrlProvider);
  }

  public static SignalingApiClient newInstance(OkHttpClient okHttpClient, String baseHttpUrl) {
    return new SignalingApiClient(okHttpClient, baseHttpUrl);
  }
}
