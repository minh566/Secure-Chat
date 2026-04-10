package com.securechat.ui.screens.contact;

import com.securechat.data.repository.FakeRepository;
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
public final class ContactsViewModel_Factory implements Factory<ContactsViewModel> {
  private final Provider<FakeRepository> repositoryProvider;

  public ContactsViewModel_Factory(Provider<FakeRepository> repositoryProvider) {
    this.repositoryProvider = repositoryProvider;
  }

  @Override
  public ContactsViewModel get() {
    return newInstance(repositoryProvider.get());
  }

  public static ContactsViewModel_Factory create(
      javax.inject.Provider<FakeRepository> repositoryProvider) {
    return new ContactsViewModel_Factory(Providers.asDaggerProvider(repositoryProvider));
  }

  public static ContactsViewModel_Factory create(Provider<FakeRepository> repositoryProvider) {
    return new ContactsViewModel_Factory(repositoryProvider);
  }

  public static ContactsViewModel newInstance(FakeRepository repository) {
    return new ContactsViewModel(repository);
  }
}
