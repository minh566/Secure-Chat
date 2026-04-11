package com.securechat;

import com.google.firebase.auth.FirebaseAuth;
import com.securechat.data.local.preferences.AppSettings;
import com.securechat.domain.repository.AuthRepository;
import com.securechat.domain.repository.CallRepository;
import dagger.MembersInjector;
import dagger.internal.DaggerGenerated;
import dagger.internal.InjectedFieldSignature;
import dagger.internal.Provider;
import dagger.internal.Providers;
import dagger.internal.QualifierMetadata;
import javax.annotation.processing.Generated;

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
public final class MainActivity_MembersInjector implements MembersInjector<MainActivity> {
  private final Provider<AuthRepository> authRepositoryProvider;

  private final Provider<CallRepository> callRepositoryProvider;

  private final Provider<AppSettings> appSettingsProvider;

  private final Provider<FirebaseAuth> firebaseAuthProvider;

  public MainActivity_MembersInjector(Provider<AuthRepository> authRepositoryProvider,
      Provider<CallRepository> callRepositoryProvider, Provider<AppSettings> appSettingsProvider,
      Provider<FirebaseAuth> firebaseAuthProvider) {
    this.authRepositoryProvider = authRepositoryProvider;
    this.callRepositoryProvider = callRepositoryProvider;
    this.appSettingsProvider = appSettingsProvider;
    this.firebaseAuthProvider = firebaseAuthProvider;
  }

  public static MembersInjector<MainActivity> create(
      Provider<AuthRepository> authRepositoryProvider,
      Provider<CallRepository> callRepositoryProvider, Provider<AppSettings> appSettingsProvider,
      Provider<FirebaseAuth> firebaseAuthProvider) {
    return new MainActivity_MembersInjector(authRepositoryProvider, callRepositoryProvider, appSettingsProvider, firebaseAuthProvider);
  }

  public static MembersInjector<MainActivity> create(
      javax.inject.Provider<AuthRepository> authRepositoryProvider,
      javax.inject.Provider<CallRepository> callRepositoryProvider,
      javax.inject.Provider<AppSettings> appSettingsProvider,
      javax.inject.Provider<FirebaseAuth> firebaseAuthProvider) {
    return new MainActivity_MembersInjector(Providers.asDaggerProvider(authRepositoryProvider), Providers.asDaggerProvider(callRepositoryProvider), Providers.asDaggerProvider(appSettingsProvider), Providers.asDaggerProvider(firebaseAuthProvider));
  }

  @Override
  public void injectMembers(MainActivity instance) {
    injectAuthRepository(instance, authRepositoryProvider.get());
    injectCallRepository(instance, callRepositoryProvider.get());
    injectAppSettings(instance, appSettingsProvider.get());
    injectFirebaseAuth(instance, firebaseAuthProvider.get());
  }

  @InjectedFieldSignature("com.securechat.MainActivity.authRepository")
  public static void injectAuthRepository(MainActivity instance, AuthRepository authRepository) {
    instance.authRepository = authRepository;
  }

  @InjectedFieldSignature("com.securechat.MainActivity.callRepository")
  public static void injectCallRepository(MainActivity instance, CallRepository callRepository) {
    instance.callRepository = callRepository;
  }

  @InjectedFieldSignature("com.securechat.MainActivity.appSettings")
  public static void injectAppSettings(MainActivity instance, AppSettings appSettings) {
    instance.appSettings = appSettings;
  }

  @InjectedFieldSignature("com.securechat.MainActivity.firebaseAuth")
  public static void injectFirebaseAuth(MainActivity instance, FirebaseAuth firebaseAuth) {
    instance.firebaseAuth = firebaseAuth;
  }
}
