package com.securechat.di.call;

import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;

@ScopeMetadata
@QualifierMetadata("javax.inject.Named")
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
public final class SignalingModule_ProvideSignalingHttpUrlFactory implements Factory<String> {
  @Override
  public String get() {
    return provideSignalingHttpUrl();
  }

  public static SignalingModule_ProvideSignalingHttpUrlFactory create() {
    return InstanceHolder.INSTANCE;
  }

  public static String provideSignalingHttpUrl() {
    return Preconditions.checkNotNullFromProvides(SignalingModule.INSTANCE.provideSignalingHttpUrl());
  }

  private static final class InstanceHolder {
    static final SignalingModule_ProvideSignalingHttpUrlFactory INSTANCE = new SignalingModule_ProvideSignalingHttpUrlFactory();
  }
}
