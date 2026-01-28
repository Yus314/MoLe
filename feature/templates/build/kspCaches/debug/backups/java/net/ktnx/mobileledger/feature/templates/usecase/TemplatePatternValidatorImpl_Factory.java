package net.ktnx.mobileledger.feature.templates.usecase;

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
public final class TemplatePatternValidatorImpl_Factory implements Factory<TemplatePatternValidatorImpl> {
  @Override
  public TemplatePatternValidatorImpl get() {
    return newInstance();
  }

  public static TemplatePatternValidatorImpl_Factory create() {
    return InstanceHolder.INSTANCE;
  }

  public static TemplatePatternValidatorImpl newInstance() {
    return new TemplatePatternValidatorImpl();
  }

  private static final class InstanceHolder {
    static final TemplatePatternValidatorImpl_Factory INSTANCE = new TemplatePatternValidatorImpl_Factory();
  }
}
