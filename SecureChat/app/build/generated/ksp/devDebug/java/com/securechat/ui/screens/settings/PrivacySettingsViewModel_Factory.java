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
public final class PrivacySettingsViewModel_Factory implements Factory<PrivacySettingsViewModel> {
  private final Provider<AppSettings> appSettingsProvider;

  public PrivacySettingsViewModel_Factory(Provider<AppSettings> appSettingsProvider) {
    this.appSettingsProvider = appSettingsProvider;
  }

  @Override
  public PrivacySettingsViewModel get() {
    return newInstance(appSettingsProvider.get());
  }

  public static PrivacySettingsViewModel_Factory create(
      javax.inject.Provider<AppSettings> appSettingsProvider) {
    return new PrivacySettingsViewModel_Factory(Providers.asDaggerProvider(appSettingsProvider));
  }

  public static PrivacySettingsViewModel_Factory create(Provider<AppSettings> appSettingsProvider) {
    return new PrivacySettingsViewModel_Factory(appSettingsProvider);
  }

  public static PrivacySettingsViewModel newInstance(AppSettings appSettings) {
    return new PrivacySettingsViewModel(appSettings);
  }
}
