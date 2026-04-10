package com.securechat.ui.screens.profile;

import com.google.firebase.storage.FirebaseStorage;
import com.securechat.domain.repository.AuthRepository;
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
public final class ProfileViewModel_Factory implements Factory<ProfileViewModel> {
  private final Provider<AuthRepository> authRepositoryProvider;

  private final Provider<FirebaseStorage> storageProvider;

  public ProfileViewModel_Factory(Provider<AuthRepository> authRepositoryProvider,
      Provider<FirebaseStorage> storageProvider) {
    this.authRepositoryProvider = authRepositoryProvider;
    this.storageProvider = storageProvider;
  }

  @Override
  public ProfileViewModel get() {
    return newInstance(authRepositoryProvider.get(), storageProvider.get());
  }

  public static ProfileViewModel_Factory create(
      javax.inject.Provider<AuthRepository> authRepositoryProvider,
      javax.inject.Provider<FirebaseStorage> storageProvider) {
    return new ProfileViewModel_Factory(Providers.asDaggerProvider(authRepositoryProvider), Providers.asDaggerProvider(storageProvider));
  }

  public static ProfileViewModel_Factory create(Provider<AuthRepository> authRepositoryProvider,
      Provider<FirebaseStorage> storageProvider) {
    return new ProfileViewModel_Factory(authRepositoryProvider, storageProvider);
  }

  public static ProfileViewModel newInstance(AuthRepository authRepository,
      FirebaseStorage storage) {
    return new ProfileViewModel(authRepository, storage);
  }
}
