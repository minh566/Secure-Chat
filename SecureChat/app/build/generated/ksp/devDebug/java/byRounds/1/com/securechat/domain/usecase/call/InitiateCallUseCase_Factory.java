package com.securechat.domain.usecase.call;

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
public final class InitiateCallUseCase_Factory implements Factory<InitiateCallUseCase> {
  private final Provider<CallRepository> callRepositoryProvider;

  public InitiateCallUseCase_Factory(Provider<CallRepository> callRepositoryProvider) {
    this.callRepositoryProvider = callRepositoryProvider;
  }

  @Override
  public InitiateCallUseCase get() {
    return newInstance(callRepositoryProvider.get());
  }

  public static InitiateCallUseCase_Factory create(
      javax.inject.Provider<CallRepository> callRepositoryProvider) {
    return new InitiateCallUseCase_Factory(Providers.asDaggerProvider(callRepositoryProvider));
  }

  public static InitiateCallUseCase_Factory create(
      Provider<CallRepository> callRepositoryProvider) {
    return new InitiateCallUseCase_Factory(callRepositoryProvider);
  }

  public static InitiateCallUseCase newInstance(CallRepository callRepository) {
    return new InitiateCallUseCase(callRepository);
  }
}
