package com.securechat.ui.screens.settings;

import com.securechat.data.local.preferences.AppSettings;
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
public final class SettingsViewModel_Factory implements Factory<SettingsViewModel> {
  private final Provider<AppSettings> appSettingsProvider;

  public SettingsViewModel_Factory(Provider<AppSettings> appSettingsProvider) {
    this.appSettingsProvider = appSettingsProvider;
  }

  @Override
  public SettingsViewModel get() {
    return newInstance(appSettingsProvider.get());
  }

  public static SettingsViewModel_Factory create(
      javax.inject.Provider<AppSettings> appSettingsProvider) {
    return new SettingsViewModel_Factory(Providers.asDaggerProvider(appSettingsProvider));
  }

  public static SettingsViewModel_Factory create(Provider<AppSettings> appSettingsProvider) {
    return new SettingsViewModel_Factory(appSettingsProvider);
  }

  public static SettingsViewModel newInstance(AppSettings appSettings) {
    return new SettingsViewModel(appSettings);
  }
}
