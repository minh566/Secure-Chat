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
public final class CreateRoomUseCase_Factory implements Factory<CreateRoomUseCase> {
  private final Provider<ChatRepository> chatRepositoryProvider;

  public CreateRoomUseCase_Factory(Provider<ChatRepository> chatRepositoryProvider) {
    this.chatRepositoryProvider = chatRepositoryProvider;
  }

  @Override
  public CreateRoomUseCase get() {
    return newInstance(chatRepositoryProvider.get());
  }

  public static CreateRoomUseCase_Factory create(
      javax.inject.Provider<ChatRepository> chatRepositoryProvider) {
    return new CreateRoomUseCase_Factory(Providers.asDaggerProvider(chatRepositoryProvider));
  }

  public static CreateRoomUseCase_Factory create(Provider<ChatRepository> chatRepositoryProvider) {
    return new CreateRoomUseCase_Factory(chatRepositoryProvider);
  }

  public static CreateRoomUseCase newInstance(ChatRepository chatRepository) {
    return new CreateRoomUseCase(chatRepository);
  }
}
