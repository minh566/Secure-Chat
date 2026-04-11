package com.securechat.domain.usecase.chat;

import com.securechat.domain.repository.ChatRepository;
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
public final class DeleteChatRoomUseCase_Factory implements Factory<DeleteChatRoomUseCase> {
  private final Provider<ChatRepository> chatRepositoryProvider;

  public DeleteChatRoomUseCase_Factory(Provider<ChatRepository> chatRepositoryProvider) {
    this.chatRepositoryProvider = chatRepositoryProvider;
  }

  @Override
  public DeleteChatRoomUseCase get() {
    return newInstance(chatRepositoryProvider.get());
  }

  public static DeleteChatRoomUseCase_Factory create(
      javax.inject.Provider<ChatRepository> chatRepositoryProvider) {
    return new DeleteChatRoomUseCase_Factory(Providers.asDaggerProvider(chatRepositoryProvider));
  }

  public static DeleteChatRoomUseCase_Factory create(
      Provider<ChatRepository> chatRepositoryProvider) {
    return new DeleteChatRoomUseCase_Factory(chatRepositoryProvider);
  }

  public static DeleteChatRoomUseCase newInstance(ChatRepository chatRepository) {
    return new DeleteChatRoomUseCase(chatRepository);
  }
}
