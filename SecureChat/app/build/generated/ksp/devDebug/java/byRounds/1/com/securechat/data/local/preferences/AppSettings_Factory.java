package com.securechat.data.local.preferences;

import android.content.Context;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Provider;
import dagger.internal.Providers;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;

@ScopeMetadata("javax.inject.Singleton")
@QualifierMetadata("dagger.hilt.android.qualifiers.ApplicationContext")
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
public final class AppSettings_Factory implements Factory<AppSettings> {
  private final Provider<Context> contextProvider;

  public AppSettings_Factory(Provider<Context> contextProvider) {
    this.contextProvider = contextProvider;
  }

  @Override
  public AppSettings get() {
    return newInstance(contextProvider.get());
  }

  public static AppSettings_Factory create(javax.inject.Provider<Context> contextProvider) {
    return new AppSettings_Factory(Providers.asDaggerProvider(contextProvider));
  }

  public static AppSettings_Factory create(Provider<Context> contextProvider) {
    return new AppSettings_Factory(contextProvider);
  }

  public static AppSettings newInstance(Context context) {
    return new AppSettings(context);
  }
}
