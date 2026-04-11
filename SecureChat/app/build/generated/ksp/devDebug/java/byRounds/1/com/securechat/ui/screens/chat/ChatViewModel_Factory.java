package com.securechat.ui.screens.chat;

import androidx.lifecycle.SavedStateHandle;
import com.securechat.domain.repository.AuthRepository;
import com.securechat.domain.repository.ChatRepository;
import com.securechat.domain.repository.UserRepository;
import com.securechat.domain.usecase.chat.AddMembersToRoomUseCase;
import com.securechat.domain.usecase.chat.GetMessagesUseCase;
import com.securechat.domain.usecase.chat.SendMessageUseCase;
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
public final class ChatViewModel_Factory implements Factory<ChatViewModel> {
  private final Provider<GetMessagesUseCase> getMessagesUseCaseProvider;

  private final Provider<SendMessageUseCase> sendMessageUseCaseProvider;

  private final Provider<AddMembersToRoomUseCase> addMembersToRoomUseCaseProvider;

  private final Provider<ChatRepository> chatRepositoryProvider;

  private final Provider<UserRepository> userRepositoryProvider;

  private final Provider<AuthRepository> authRepositoryProvider;

  private final Provider<SavedStateHandle> savedStateHandleProvider;

  public ChatViewModel_Factory(Provider<GetMessagesUseCase> getMessagesUseCaseProvider,
      Provider<SendMessageUseCase> sendMessageUseCaseProvider,
      Provider<AddMembersToRoomUseCase> addMembersToRoomUseCaseProvider,
      Provider<ChatRepository> chatRepositoryProvider,
      Provider<UserRepository> userRepositoryProvider,
      Provider<AuthRepository> authRepositoryProvider,
      Provider<SavedStateHandle> savedStateHandleProvider) {
    this.getMessagesUseCaseProvider = getMessagesUseCaseProvider;
    this.sendMessageUseCaseProvider = sendMessageUseCaseProvider;
    this.addMembersToRoomUseCaseProvider = addMembersToRoomUseCaseProvider;
    this.chatRepositoryProvider = chatRepositoryProvider;
    this.userRepositoryProvider = userRepositoryProvider;
    this.authRepositoryProvider = authRepositoryProvider;
    this.savedStateHandleProvider = savedStateHandleProvider;
  }

  @Override
  public ChatViewModel get() {
    return newInstance(getMessagesUseCaseProvider.get(), sendMessageUseCaseProvider.get(), addMembersToRoomUseCaseProvider.get(), chatRepositoryProvider.get(), userRepositoryProvider.get(), authRepositoryProvider.get(), savedStateHandleProvider.get());
  }

  public static ChatViewModel_Factory create(
      javax.inject.Provider<GetMessagesUseCase> getMessagesUseCaseProvider,
      javax.inject.Provider<SendMessageUseCase> sendMessageUseCaseProvider,
      javax.inject.Provider<AddMembersToRoomUseCase> addMembersToRoomUseCaseProvider,
      javax.inject.Provider<ChatRepository> chatRepositoryProvider,
      javax.inject.Provider<UserRepository> userRepositoryProvider,
      javax.inject.Provider<AuthRepository> authRepositoryProvider,
      javax.inject.Provider<SavedStateHandle> savedStateHandleProvider) {
    return new ChatViewModel_Factory(Providers.asDaggerProvider(getMessagesUseCaseProvider), Providers.asDaggerProvider(sendMessageUseCaseProvider), Providers.asDaggerProvider(addMembersToRoomUseCaseProvider), Providers.asDaggerProvider(chatRepositoryProvider), Providers.asDaggerProvider(userRepositoryProvider), Providers.asDaggerProvider(authRepositoryProvider), Providers.asDaggerProvider(savedStateHandleProvider));
  }

  public static ChatViewModel_Factory create(
      Provider<GetMessagesUseCase> getMessagesUseCaseProvider,
      Provider<SendMessageUseCase> sendMessageUseCaseProvider,
      Provider<AddMembersToRoomUseCase> addMembersToRoomUseCaseProvider,
      Provider<ChatRepository> chatRepositoryProvider,
      Provider<UserRepository> userRepositoryProvider,
      Provider<AuthRepository> authRepositoryProvider,
      Provider<SavedStateHandle> savedStateHandleProvider) {
    return new ChatViewModel_Factory(getMessagesUseCaseProvider, sendMessageUseCaseProvider, addMembersToRoomUseCaseProvider, chatRepositoryProvider, userRepositoryProvider, authRepositoryProvider, savedStateHandleProvider);
  }

  public static ChatViewModel newInstance(GetMessagesUseCase getMessagesUseCase,
      SendMessageUseCase sendMessageUseCase, AddMembersToRoomUseCase addMembersToRoomUseCase,
      ChatRepository chatRepository, UserRepository userRepository, AuthRepository authRepository,
      SavedStateHandle savedStateHandle) {
    return new ChatViewModel(getMessagesUseCase, sendMessageUseCase, addMembersToRoomUseCase, chatRepository, userRepository, authRepository, savedStateHandle);
  }
}
