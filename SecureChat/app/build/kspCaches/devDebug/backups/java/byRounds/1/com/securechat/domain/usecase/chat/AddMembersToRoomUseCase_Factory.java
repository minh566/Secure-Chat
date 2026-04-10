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
public final class AddMembersToRoomUseCase_Factory implements Factory<AddMembersToRoomUseCase> {
  private final Provider<ChatRepository> chatRepositoryProvider;

  public AddMembersToRoomUseCase_Factory(Provider<ChatRepository> chatRepositoryProvider) {
    this.chatRepositoryProvider = chatRepositoryProvider;
  }

  @Override
  public AddMembersToRoomUseCase get() {
    return newInstance(chatRepositoryProvider.get());
  }

  public static AddMembersToRoomUseCase_Factory create(
      javax.inject.Provider<ChatRepository> chatRepositoryProvider) {
    return new AddMembersToRoomUseCase_Factory(Providers.asDaggerProvider(chatRepositoryProvider));
  }

  public static AddMembersToRoomUseCase_Factory create(
      Provider<ChatRepository> chatRepositoryProvider) {
    return new AddMembersToRoomUseCase_Factory(chatRepositoryProvider);
  }

  public static AddMembersToRoomUseCase newInstance(ChatRepository chatRepository) {
    return new AddMembersToRoomUseCase(chatRepository);
  }
}
