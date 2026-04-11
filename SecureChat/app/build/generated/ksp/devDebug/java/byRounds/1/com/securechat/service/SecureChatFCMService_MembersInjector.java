package com.securechat.service;

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
public final class SecureChatFCMService_MembersInjector implements MembersInjector<SecureChatFCMService> {
  private final Provider<AuthRepository> authRepositoryProvider;

  private final Provider<CallRepository> callRepositoryProvider;

  public SecureChatFCMService_MembersInjector(Provider<AuthRepository> authRepositoryProvider,
      Provider<CallRepository> callRepositoryProvider) {
    this.authRepositoryProvider = authRepositoryProvider;
    this.callRepositoryProvider = callRepositoryProvider;
  }

  public static MembersInjector<SecureChatFCMService> create(
      Provider<AuthRepository> authRepositoryProvider,
      Provider<CallRepository> callRepositoryProvider) {
    return new SecureChatFCMService_MembersInjector(authRepositoryProvider, callRepositoryProvider);
  }

  public static MembersInjector<SecureChatFCMService> create(
      javax.inject.Provider<AuthRepository> authRepositoryProvider,
      javax.inject.Provider<CallRepository> callRepositoryProvider) {
    return new SecureChatFCMService_MembersInjector(Providers.asDaggerProvider(authRepositoryProvider), Providers.asDaggerProvider(callRepositoryProvider));
  }

  @Override
  public void injectMembers(SecureChatFCMService instance) {
    injectAuthRepository(instance, authRepositoryProvider.get());
    injectCallRepository(instance, callRepositoryProvider.get());
  }

  @InjectedFieldSignature("com.securechat.service.SecureChatFCMService.authRepository")
  public static void injectAuthRepository(SecureChatFCMService instance,
      AuthRepository authRepository) {
    instance.authRepository = authRepository;
  }

  @InjectedFieldSignature("com.securechat.service.SecureChatFCMService.callRepository")
  public static void injectCallRepository(SecureChatFCMService instance,
      CallRepository callRepository) {
    instance.callRepository = callRepository;
  }
}
