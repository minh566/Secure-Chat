package com.securechat.data.remote.webrtc;

import android.content.Context;
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

  public WebRTCManager_Factory(Provider<Context> contextProvider,
      Provider<CallRepository> callRepositoryProvider) {
    this.contextProvider = contextProvider;
    this.callRepositoryProvider = callRepositoryProvider;
  }

  @Override
  public WebRTCManager get() {
    return newInstance(contextProvider.get(), callRepositoryProvider.get());
  }

  public static WebRTCManager_Factory create(javax.inject.Provider<Context> contextProvider,
      javax.inject.Provider<CallRepository> callRepositoryProvider) {
    return new WebRTCManager_Factory(Providers.asDaggerProvider(contextProvider), Providers.asDaggerProvider(callRepositoryProvider));
  }

  public static WebRTCManager_Factory create(Provider<Context> contextProvider,
      Provider<CallRepository> callRepositoryProvider) {
    return new WebRTCManager_Factory(contextProvider, callRepositoryProvider);
  }

  public static WebRTCManager newInstance(Context context, CallRepository callRepository) {
    return new WebRTCManager(context, callRepository);
  }
}
