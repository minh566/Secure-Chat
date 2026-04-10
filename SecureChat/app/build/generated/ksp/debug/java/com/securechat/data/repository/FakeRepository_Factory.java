package com.securechat.data.repository;

import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;

@ScopeMetadata("javax.inject.Singleton")
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
public final class FakeRepository_Factory implements Factory<FakeRepository> {
  @Override
  public FakeRepository get() {
    return newInstance();
  }

  public static FakeRepository_Factory create() {
    return InstanceHolder.INSTANCE;
  }

  public static FakeRepository newInstance() {
    return new FakeRepository();
  }

  private static final class InstanceHolder {
    static final FakeRepository_Factory INSTANCE = new FakeRepository_Factory();
  }
}
