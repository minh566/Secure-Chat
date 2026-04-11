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
public final class SignUpUseCase_Factory implements Factory<SignUpUseCase> {
  private final Provider<AuthRepository> authRepositoryProvider;

  public SignUpUseCase_Factory(Provider<AuthRepository> authRepositoryProvider) {
    this.authRepositoryProvider = authRepositoryProvider;
  }

  @Override
  public SignUpUseCase get() {
    return newInstance(authRepositoryProvider.get());
  }

  public static SignUpUseCase_Factory create(
      javax.inject.Provider<AuthRepository> authRepositoryProvider) {
    return new SignUpUseCase_Factory(Providers.asDaggerProvider(authRepositoryProvider));
  }

  public static SignUpUseCase_Factory create(Provider<AuthRepository> authRepositoryProvider) {
    return new SignUpUseCase_Factory(authRepositoryProvider);
  }

  public static SignUpUseCase newInstance(AuthRepository authRepository) {
    return new SignUpUseCase(authRepository);
  }
}
