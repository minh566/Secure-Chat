package com.securechat.domain.usecase.auth;

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
public final class SignOutUseCase_Factory implements Factory<SignOutUseCase> {
  private final Provider<AuthRepository> authRepositoryProvider;

  public SignOutUseCase_Factory(Provider<AuthRepository> authRepositoryProvider) {
    this.authRepositoryProvider = authRepositoryProvider;
  }

  @Override
  public SignOutUseCase get() {
    return newInstance(authRepositoryProvider.get());
  }

  public static SignOutUseCase_Factory create(
      javax.inject.Provider<AuthRepository> authRepositoryProvider) {
    return new SignOutUseCase_Factory(Providers.asDaggerProvider(authRepositoryProvider));
  }

  public static SignOutUseCase_Factory create(Provider<AuthRepository> authRepositoryProvider) {
    return new SignOutUseCase_Factory(authRepositoryProvider);
  }

  public static SignOutUseCase newInstance(AuthRepository authRepository) {
    return new SignOutUseCase(authRepository);
  }
}
