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
public final class SignalingModule_ProvideSignalingWsUrlFactory implements Factory<String> {
  @Override
  public String get() {
    return provideSignalingWsUrl();
  }

  public static SignalingModule_ProvideSignalingWsUrlFactory create() {
    return InstanceHolder.INSTANCE;
  }

  public static String provideSignalingWsUrl() {
    return Preconditions.checkNotNullFromProvides(SignalingModule.INSTANCE.provideSignalingWsUrl());
  }

  private static final class InstanceHolder {
    static final SignalingModule_ProvideSignalingWsUrlFactory INSTANCE = new SignalingModule_ProvideSignalingWsUrlFactory();
  }
}
