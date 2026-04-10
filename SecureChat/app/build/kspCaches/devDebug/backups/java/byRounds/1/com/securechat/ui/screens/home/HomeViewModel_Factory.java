package com.securechat.ui.screens.home;

import com.securechat.domain.repository.AuthRepository;
import com.securechat.domain.repository.ChatRepository;
import com.securechat.domain.repository.UserRepository;
import com.securechat.domain.usecase.auth.SignOutUseCase;
import com.securechat.domain.usecase.chat.CreateRoomUseCase;
import com.securechat.domain.usecase.chat.GetChatRoomsUseCase;
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
public final class HomeViewModel_Factory implements Factory<HomeViewModel> {
  private final Provider<GetChatRoomsUseCase> getChatRoomsUseCaseProvider;

  private final Provider<CreateRoomUseCase> createRoomUseCaseProvider;

  private final Provider<ChatRepository> chatRepositoryProvider;

  private final Provider<SignOutUseCase> signOutUseCaseProvider;

  private final Provider<AuthRepository> authRepositoryProvider;

  private final Provider<UserRepository> userRepositoryProvider;

  public HomeViewModel_Factory(Provider<GetChatRoomsUseCase> getChatRoomsUseCaseProvider,
      Provider<CreateRoomUseCase> createRoomUseCaseProvider,
      Provider<ChatRepository> chatRepositoryProvider,
      Provider<SignOutUseCase> signOutUseCaseProvider,
      Provider<AuthRepository> authRepositoryProvider,
      Provider<UserRepository> userRepositoryProvider) {
    this.getChatRoomsUseCaseProvider = getChatRoomsUseCaseProvider;
    this.createRoomUseCaseProvider = createRoomUseCaseProvider;
    this.chatRepositoryProvider = chatRepositoryProvider;
    this.signOutUseCaseProvider = signOutUseCaseProvider;
    this.authRepositoryProvider = authRepositoryProvider;
    this.userRepositoryProvider = userRepositoryProvider;
  }

  @Override
  public HomeViewModel get() {
    return newInstance(getChatRoomsUseCaseProvider.get(), createRoomUseCaseProvider.get(), chatRepositoryProvider.get(), signOutUseCaseProvider.get(), authRepositoryProvider.get(), userRepositoryProvider.get());
  }

  public static HomeViewModel_Factory create(
      javax.inject.Provider<GetChatRoomsUseCase> getChatRoomsUseCaseProvider,
      javax.inject.Provider<CreateRoomUseCase> createRoomUseCaseProvider,
      javax.inject.Provider<ChatRepository> chatRepositoryProvider,
      javax.inject.Provider<SignOutUseCase> signOutUseCaseProvider,
      javax.inject.Provider<AuthRepository> authRepositoryProvider,
      javax.inject.Provider<UserRepository> userRepositoryProvider) {
    return new HomeViewModel_Factory(Providers.asDaggerProvider(getChatRoomsUseCaseProvider), Providers.asDaggerProvider(createRoomUseCaseProvider), Providers.asDaggerProvider(chatRepositoryProvider), Providers.asDaggerProvider(signOutUseCaseProvider), Providers.asDaggerProvider(authRepositoryProvider), Providers.asDaggerProvider(userRepositoryProvider));
  }

  public static HomeViewModel_Factory create(
      Provider<GetChatRoomsUseCase> getChatRoomsUseCaseProvider,
      Provider<CreateRoomUseCase> createRoomUseCaseProvider,
      Provider<ChatRepository> chatRepositoryProvider,
      Provider<SignOutUseCase> signOutUseCaseProvider,
      Provider<AuthRepository> authRepositoryProvider,
      Provider<UserRepository> userRepositoryProvider) {
    return new HomeViewModel_Factory(getChatRoomsUseCaseProvider, createRoomUseCaseProvider, chatRepositoryProvider, signOutUseCaseProvider, authRepositoryProvider, userRepositoryProvider);
  }

  public static HomeViewModel newInstance(GetChatRoomsUseCase getChatRoomsUseCase,
      CreateRoomUseCase createRoomUseCase, ChatRepository chatRepository,
      SignOutUseCase signOutUseCase, AuthRepository authRepository, UserRepository userRepository) {
    return new HomeViewModel(getChatRoomsUseCase, createRoomUseCase, chatRepository, signOutUseCase, authRepository, userRepository);
  }
}
