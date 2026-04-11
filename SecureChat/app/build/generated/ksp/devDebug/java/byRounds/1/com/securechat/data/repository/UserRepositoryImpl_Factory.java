package com.securechat.data.repository;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
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
public final class UserRepositoryImpl_Factory implements Factory<UserRepositoryImpl> {
  private final Provider<FirebaseFirestore> firestoreProvider;

  private final Provider<FirebaseAuth> firebaseAuthProvider;

  public UserRepositoryImpl_Factory(Provider<FirebaseFirestore> firestoreProvider,
      Provider<FirebaseAuth> firebaseAuthProvider) {
    this.firestoreProvider = firestoreProvider;
    this.firebaseAuthProvider = firebaseAuthProvider;
  }

  @Override
  public UserRepositoryImpl get() {
    return newInstance(firestoreProvider.get(), firebaseAuthProvider.get());
  }

  public static UserRepositoryImpl_Factory create(
      javax.inject.Provider<FirebaseFirestore> firestoreProvider,
      javax.inject.Provider<FirebaseAuth> firebaseAuthProvider) {
    return new UserRepositoryImpl_Factory(Providers.asDaggerProvider(firestoreProvider), Providers.asDaggerProvider(firebaseAuthProvider));
  }

  public static UserRepositoryImpl_Factory create(Provider<FirebaseFirestore> firestoreProvider,
      Provider<FirebaseAuth> firebaseAuthProvider) {
    return new UserRepositoryImpl_Factory(firestoreProvider, firebaseAuthProvider);
  }

  public static UserRepositoryImpl newInstance(FirebaseFirestore firestore,
      FirebaseAuth firebaseAuth) {
    return new UserRepositoryImpl(firestore, firebaseAuth);
  }
}
