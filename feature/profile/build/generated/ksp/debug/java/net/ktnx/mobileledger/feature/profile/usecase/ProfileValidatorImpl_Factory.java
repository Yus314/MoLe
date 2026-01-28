package net.ktnx.mobileledger.feature.profile.usecase;

import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
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
public final class ProfileValidatorImpl_Factory implements Factory<ProfileValidatorImpl> {
  @Override
  public ProfileValidatorImpl get() {
    return newInstance();
  }

  public static ProfileValidatorImpl_Factory create() {
    return InstanceHolder.INSTANCE;
  }

  public static ProfileValidatorImpl newInstance() {
    return new ProfileValidatorImpl();
  }

  private static final class InstanceHolder {
    static final ProfileValidatorImpl_Factory INSTANCE = new ProfileValidatorImpl_Factory();
  }
}
