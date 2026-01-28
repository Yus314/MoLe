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
public final class TemplateMatcherImpl_Factory implements Factory<TemplateMatcherImpl> {
  @Override
  public TemplateMatcherImpl get() {
    return newInstance();
  }

  public static TemplateMatcherImpl_Factory create() {
    return InstanceHolder.INSTANCE;
  }

  public static TemplateMatcherImpl newInstance() {
    return new TemplateMatcherImpl();
  }

  private static final class InstanceHolder {
    static final TemplateMatcherImpl_Factory INSTANCE = new TemplateMatcherImpl_Factory();
  }
}
