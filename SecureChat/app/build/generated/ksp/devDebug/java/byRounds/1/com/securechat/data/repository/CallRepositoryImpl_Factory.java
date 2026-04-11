package com.securechat.data.repository;

import com.google.firebase.firestore.FirebaseFirestore;
import com.securechat.data.remote.signaling.SignalingApiClient;
import com.securechat.data.remote.signaling.SignalingWebSocketClient;
import com.securechat.domain.repository.AuthRepository;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Provider;
import dagger.internal.Providers;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;

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
public final class CallRepositoryImpl_Factory implements Factory<CallRepositoryImpl> {
  private final Provider<FirebaseFirestore> firestoreProvider;

  private final Provider<AuthRepository> authRepositoryProvider;

  private final Provider<SignalingApiClient> signalingApiClientProvider;

  private final Provider<SignalingWebSocketClient> signalingWebSocketClientProvider;

  public CallRepositoryImpl_Factory(Provider<FirebaseFirestore> firestoreProvider,
      Provider<AuthRepository> authRepositoryProvider,
      Provider<SignalingApiClient> signalingApiClientProvider,
      Provider<SignalingWebSocketClient> signalingWebSocketClientProvider) {
    this.firestoreProvider = firestoreProvider;
    this.authRepositoryProvider = authRepositoryProvider;
    this.signalingApiClientProvider = signalingApiClientProvider;
    this.signalingWebSocketClientProvider = signalingWebSocketClientProvider;
  }

  @Override
  public CallRepositoryImpl get() {
    return newInstance(firestoreProvider.get(), authRepositoryProvider.get(), signalingApiClientProvider.get(), signalingWebSocketClientProvider.get());
  }

  public static CallRepositoryImpl_Factory create(
      javax.inject.Provider<FirebaseFirestore> firestoreProvider,
      javax.inject.Provider<AuthRepository> authRepositoryProvider,
      javax.inject.Provider<SignalingApiClient> signalingApiClientProvider,
      javax.inject.Provider<SignalingWebSocketClient> signalingWebSocketClientProvider) {
    return new CallRepositoryImpl_Factory(Providers.asDaggerProvider(firestoreProvider), Providers.asDaggerProvider(authRepositoryProvider), Providers.asDaggerProvider(signalingApiClientProvider), Providers.asDaggerProvider(signalingWebSocketClientProvider));
  }

  public static CallRepositoryImpl_Factory create(Provider<FirebaseFirestore> firestoreProvider,
      Provider<AuthRepository> authRepositoryProvider,
      Provider<SignalingApiClient> signalingApiClientProvider,
      Provider<SignalingWebSocketClient> signalingWebSocketClientProvider) {
    return new CallRepositoryImpl_Factory(firestoreProvider, authRepositoryProvider, signalingApiClientProvider, signalingWebSocketClientProvider);
  }

  public static CallRepositoryImpl newInstance(FirebaseFirestore firestore,
      AuthRepository authRepository, SignalingApiClient signalingApiClient,
      SignalingWebSocketClient signalingWebSocketClient) {
    return new CallRepositoryImpl(firestore, authRepository, signalingApiClient, signalingWebSocketClient);
  }
}
