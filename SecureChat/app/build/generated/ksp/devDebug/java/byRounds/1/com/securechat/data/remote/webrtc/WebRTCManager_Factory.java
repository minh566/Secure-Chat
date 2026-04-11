package com.securechat.data.remote.webrtc;

import android.content.Context;
import com.securechat.data.remote.signaling.SignalingApiClient;
import com.securechat.domain.repository.CallRepository;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Provider;
import dagger.internal.Providers;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;

@ScopeMetadata("javax.inject.Singleton")
@QualifierMetadata("dagger.hilt.android.qualifiers.ApplicationContext")
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
public final class WebRTCManager_Factory implements Factory<WebRTCManager> {
  private final Provider<Context> contextProvider;

  private final Provider<CallRepository> callRepositoryProvider;

  private final Provider<SignalingApiClient> signalingApiClientProvider;

  public WebRTCManager_Factory(Provider<Context> contextProvider,
      Provider<CallRepository> callRepositoryProvider,
      Provider<SignalingApiClient> signalingApiClientProvider) {
    this.contextProvider = contextProvider;
    this.callRepositoryProvider = callRepositoryProvider;
    this.signalingApiClientProvider = signalingApiClientProvider;
  }

  @Override
  public WebRTCManager get() {
    return newInstance(contextProvider.get(), callRepositoryProvider.get(), signalingApiClientProvider.get());
  }

  public static WebRTCManager_Factory create(javax.inject.Provider<Context> contextProvider,
      javax.inject.Provider<CallRepository> callRepositoryProvider,
      javax.inject.Provider<SignalingApiClient> signalingApiClientProvider) {
    return new WebRTCManager_Factory(Providers.asDaggerProvider(contextProvider), Providers.asDaggerProvider(callRepositoryProvider), Providers.asDaggerProvider(signalingApiClientProvider));
  }

  public static WebRTCManager_Factory create(Provider<Context> contextProvider,
      Provider<CallRepository> callRepositoryProvider,
      Provider<SignalingApiClient> signalingApiClientProvider) {
    return new WebRTCManager_Factory(contextProvider, callRepositoryProvider, signalingApiClientProvider);
  }

  public static WebRTCManager newInstance(Context context, CallRepository callRepository,
      SignalingApiClient signalingApiClient) {
    return new WebRTCManager(context, callRepository, signalingApiClient);
  }
}
