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
public final class ObserveIncomingCallUseCase_Factory implements Factory<ObserveIncomingCallUseCase> {
  private final Provider<CallRepository> callRepositoryProvider;

  public ObserveIncomingCallUseCase_Factory(Provider<CallRepository> callRepositoryProvider) {
    this.callRepositoryProvider = callRepositoryProvider;
  }

  @Override
  public ObserveIncomingCallUseCase get() {
    return newInstance(callRepositoryProvider.get());
  }

  public static ObserveIncomingCallUseCase_Factory create(
      javax.inject.Provider<CallRepository> callRepositoryProvider) {
    return new ObserveIncomingCallUseCase_Factory(Providers.asDaggerProvider(callRepositoryProvider));
  }

  public static ObserveIncomingCallUseCase_Factory create(
      Provider<CallRepository> callRepositoryProvider) {
    return new ObserveIncomingCallUseCase_Factory(callRepositoryProvider);
  }

  public static ObserveIncomingCallUseCase newInstance(CallRepository callRepository) {
    return new ObserveIncomingCallUseCase(callRepository);
  }
}
