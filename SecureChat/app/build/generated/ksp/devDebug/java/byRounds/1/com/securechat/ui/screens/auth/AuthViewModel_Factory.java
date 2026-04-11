package com.securechat.ui.screens.auth;

import com.securechat.domain.usecase.auth.SignInUseCase;
import com.securechat.domain.usecase.auth.SignOutUseCase;
import com.securechat.domain.usecase.auth.SignUpUseCase;
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
public final class AuthViewModel_Factory implements Factory<AuthViewModel> {
  private final Provider<SignInUseCase> signInUseCaseProvider;

  private final Provider<SignUpUseCase> signUpUseCaseProvider;

  private final Provider<SignOutUseCase> signOutUseCaseProvider;

  public AuthViewModel_Factory(Provider<SignInUseCase> signInUseCaseProvider,
      Provider<SignUpUseCase> signUpUseCaseProvider,
      Provider<SignOutUseCase> signOutUseCaseProvider) {
    this.signInUseCaseProvider = signInUseCaseProvider;
    this.signUpUseCaseProvider = signUpUseCaseProvider;
    this.signOutUseCaseProvider = signOutUseCaseProvider;
  }

  @Override
  public AuthViewModel get() {
    return newInstance(signInUseCaseProvider.get(), signUpUseCaseProvider.get(), signOutUseCaseProvider.get());
  }

  public static AuthViewModel_Factory create(
      javax.inject.Provider<SignInUseCase> signInUseCaseProvider,
      javax.inject.Provider<SignUpUseCase> signUpUseCaseProvider,
      javax.inject.Provider<SignOutUseCase> signOutUseCaseProvider) {
    return new AuthViewModel_Factory(Providers.asDaggerProvider(signInUseCaseProvider), Providers.asDaggerProvider(signUpUseCaseProvider), Providers.asDaggerProvider(signOutUseCaseProvider));
  }

  public static AuthViewModel_Factory create(Provider<SignInUseCase> signInUseCaseProvider,
      Provider<SignUpUseCase> signUpUseCaseProvider,
      Provider<SignOutUseCase> signOutUseCaseProvider) {
    return new AuthViewModel_Factory(signInUseCaseProvider, signUpUseCaseProvider, signOutUseCaseProvider);
  }

  public static AuthViewModel newInstance(SignInUseCase signInUseCase, SignUpUseCase signUpUseCase,
      SignOutUseCase signOutUseCase) {
    return new AuthViewModel(signInUseCase, signUpUseCase, signOutUseCase);
  }
}
