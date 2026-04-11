package com.securechat.data.repository;

import android.content.Context;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.securechat.data.local.dao.MessageDao;
import com.securechat.data.remote.signaling.SignalingApiClient;
import com.securechat.domain.repository.UserRepository;
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
public final class ChatRepositoryImpl_Factory implements Factory<ChatRepositoryImpl> {
  private final Provider<Context> contextProvider;

  private final Provider<FirebaseAuth> firebaseAuthProvider;

  private final Provider<FirebaseFirestore> firestoreProvider;

  private final Provider<FirebaseStorage> storageProvider;

  private final Provider<MessageDao> messageDaoProvider;

  private final Provider<UserRepository> userRepositoryProvider;

  private final Provider<SignalingApiClient> signalingApiClientProvider;

  public ChatRepositoryImpl_Factory(Provider<Context> contextProvider,
      Provider<FirebaseAuth> firebaseAuthProvider, Provider<FirebaseFirestore> firestoreProvider,
      Provider<FirebaseStorage> storageProvider, Provider<MessageDao> messageDaoProvider,
      Provider<UserRepository> userRepositoryProvider,
      Provider<SignalingApiClient> signalingApiClientProvider) {
    this.contextProvider = contextProvider;
    this.firebaseAuthProvider = firebaseAuthProvider;
    this.firestoreProvider = firestoreProvider;
    this.storageProvider = storageProvider;
    this.messageDaoProvider = messageDaoProvider;
    this.userRepositoryProvider = userRepositoryProvider;
    this.signalingApiClientProvider = signalingApiClientProvider;
  }

  @Override
  public ChatRepositoryImpl get() {
    return newInstance(contextProvider.get(), firebaseAuthProvider.get(), firestoreProvider.get(), storageProvider.get(), messageDaoProvider.get(), userRepositoryProvider.get(), signalingApiClientProvider.get());
  }

  public static ChatRepositoryImpl_Factory create(javax.inject.Provider<Context> contextProvider,
      javax.inject.Provider<FirebaseAuth> firebaseAuthProvider,
      javax.inject.Provider<FirebaseFirestore> firestoreProvider,
      javax.inject.Provider<FirebaseStorage> storageProvider,
      javax.inject.Provider<MessageDao> messageDaoProvider,
      javax.inject.Provider<UserRepository> userRepositoryProvider,
      javax.inject.Provider<SignalingApiClient> signalingApiClientProvider) {
    return new ChatRepositoryImpl_Factory(Providers.asDaggerProvider(contextProvider), Providers.asDaggerProvider(firebaseAuthProvider), Providers.asDaggerProvider(firestoreProvider), Providers.asDaggerProvider(storageProvider), Providers.asDaggerProvider(messageDaoProvider), Providers.asDaggerProvider(userRepositoryProvider), Providers.asDaggerProvider(signalingApiClientProvider));
  }

  public static ChatRepositoryImpl_Factory create(Provider<Context> contextProvider,
      Provider<FirebaseAuth> firebaseAuthProvider, Provider<FirebaseFirestore> firestoreProvider,
      Provider<FirebaseStorage> storageProvider, Provider<MessageDao> messageDaoProvider,
      Provider<UserRepository> userRepositoryProvider,
      Provider<SignalingApiClient> signalingApiClientProvider) {
    return new ChatRepositoryImpl_Factory(contextProvider, firebaseAuthProvider, firestoreProvider, storageProvider, messageDaoProvider, userRepositoryProvider, signalingApiClientProvider);
  }

  public static ChatRepositoryImpl newInstance(Context context, FirebaseAuth firebaseAuth,
      FirebaseFirestore firestore, FirebaseStorage storage, MessageDao messageDao,
      UserRepository userRepository, SignalingApiClient signalingApiClient) {
    return new ChatRepositoryImpl(context, firebaseAuth, firestore, storage, messageDao, userRepository, signalingApiClient);
  }
}
