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
public final class AuthRepositoryImpl_Factory implements Factory<AuthRepositoryImpl> {
  private final Provider<FirebaseAuth> firebaseAuthProvider;

  private final Provider<FirebaseFirestore> firestoreProvider;

  public AuthRepositoryImpl_Factory(Provider<FirebaseAuth> firebaseAuthProvider,
      Provider<FirebaseFirestore> firestoreProvider) {
    this.firebaseAuthProvider = firebaseAuthProvider;
    this.firestoreProvider = firestoreProvider;
  }

  @Override
  public AuthRepositoryImpl get() {
    return newInstance(firebaseAuthProvider.get(), firestoreProvider.get());
  }

  public static AuthRepositoryImpl_Factory create(
      javax.inject.Provider<FirebaseAuth> firebaseAuthProvider,
      javax.inject.Provider<FirebaseFirestore> firestoreProvider) {
    return new AuthRepositoryImpl_Factory(Providers.asDaggerProvider(firebaseAuthProvider), Providers.asDaggerProvider(firestoreProvider));
  }

  public static AuthRepositoryImpl_Factory create(Provider<FirebaseAuth> firebaseAuthProvider,
      Provider<FirebaseFirestore> firestoreProvider) {
    return new AuthRepositoryImpl_Factory(firebaseAuthProvider, firestoreProvider);
  }

  public static AuthRepositoryImpl newInstance(FirebaseAuth firebaseAuth,
      FirebaseFirestore firestore) {
    return new AuthRepositoryImpl(firebaseAuth, firestore);
  }
}
