package com.securechat.ui.screens.call;

import androidx.lifecycle.SavedStateHandle;
import com.securechat.data.remote.webrtc.WebRTCManager;
import com.securechat.domain.repository.AuthRepository;
import com.securechat.domain.repository.CallRepository;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Provider;
import dagger.internal.Providers;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;

@ScopeMetadata
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
public final class CallViewModel_Factory implements Factory<CallViewModel> {
  private final Provider<WebRTCManager> webRTCManagerProvider;

  private final Provider<CallRepository> callRepositoryProvider;

  private final Provider<AuthRepository> authRepositoryProvider;

  private final Provider<SavedStateHandle> savedStateHandleProvider;

  public CallViewModel_Factory(Provider<WebRTCManager> webRTCManagerProvider,
      Provider<CallRepository> callRepositoryProvider,
      Provider<AuthRepository> authRepositoryProvider,
      Provider<SavedStateHandle> savedStateHandleProvider) {
    this.webRTCManagerProvider = webRTCManagerProvider;
    this.callRepositoryProvider = callRepositoryProvider;
    this.authRepositoryProvider = authRepositoryProvider;
    this.savedStateHandleProvider = savedStateHandleProvider;
  }

  @Override
  public CallViewModel get() {
    return newInstance(webRTCManagerProvider.get(), callRepositoryProvider.get(), authRepositoryProvider.get(), savedStateHandleProvider.get());
  }

  public static CallViewModel_Factory create(
      javax.inject.Provider<WebRTCManager> webRTCManagerProvider,
      javax.inject.Provider<CallRepository> callRepositoryProvider,
      javax.inject.Provider<AuthRepository> authRepositoryProvider,
      javax.inject.Provider<SavedStateHandle> savedStateHandleProvider) {
    return new CallViewModel_Factory(Providers.asDaggerProvider(webRTCManagerProvider), Providers.asDaggerProvider(callRepositoryProvider), Providers.asDaggerProvider(authRepositoryProvider), Providers.asDaggerProvider(savedStateHandleProvider));
  }

  public static CallViewModel_Factory create(Provider<WebRTCManager> webRTCManagerProvider,
      Provider<CallRepository> callRepositoryProvider,
      Provider<AuthRepository> authRepositoryProvider,
      Provider<SavedStateHandle> savedStateHandleProvider) {
    return new CallViewModel_Factory(webRTCManagerProvider, callRepositoryProvider, authRepositoryProvider, savedStateHandleProvider);
  }

  public static CallViewModel newInstance(WebRTCManager webRTCManager,
      CallRepository callRepository, AuthRepository authRepository,
      SavedStateHandle savedStateHandle) {
    return new CallViewModel(webRTCManager, callRepository, authRepository, savedStateHandle);
  }
}
