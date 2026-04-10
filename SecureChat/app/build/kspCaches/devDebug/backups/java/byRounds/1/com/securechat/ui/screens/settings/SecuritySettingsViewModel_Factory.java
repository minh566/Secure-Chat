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
public final class SecuritySettingsViewModel_Factory implements Factory<SecuritySettingsViewModel> {
  private final Provider<AppSettings> appSettingsProvider;

  public SecuritySettingsViewModel_Factory(Provider<AppSettings> appSettingsProvider) {
    this.appSettingsProvider = appSettingsProvider;
  }

  @Override
  public SecuritySettingsViewModel get() {
    return newInstance(appSettingsProvider.get());
  }

  public static SecuritySettingsViewModel_Factory create(
      javax.inject.Provider<AppSettings> appSettingsProvider) {
    return new SecuritySettingsViewModel_Factory(Providers.asDaggerProvider(appSettingsProvider));
  }

  public static SecuritySettingsViewModel_Factory create(
      Provider<AppSettings> appSettingsProvider) {
    return new SecuritySettingsViewModel_Factory(appSettingsProvider);
  }

  public static SecuritySettingsViewModel newInstance(AppSettings appSettings) {
    return new SecuritySettingsViewModel(appSettings);
  }
}
