package net.ktnx.mobileledger.feature.templates.usecase;

import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Provider;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import net.ktnx.mobileledger.core.domain.repository.TemplateRepository;

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
public final class SaveTemplateUseCaseImpl_Factory implements Factory<SaveTemplateUseCaseImpl> {
  private final Provider<TemplateRepository> templateRepositoryProvider;

  private SaveTemplateUseCaseImpl_Factory(Provider<TemplateRepository> templateRepositoryProvider) {
    this.templateRepositoryProvider = templateRepositoryProvider;
  }

  @Override
  public SaveTemplateUseCaseImpl get() {
    return newInstance(templateRepositoryProvider.get());
  }

  public static SaveTemplateUseCaseImpl_Factory create(
      Provider<TemplateRepository> templateRepositoryProvider) {
    return new SaveTemplateUseCaseImpl_Factory(templateRepositoryProvider);
  }

  public static SaveTemplateUseCaseImpl newInstance(TemplateRepository templateRepository) {
    return new SaveTemplateUseCaseImpl(templateRepository);
  }
}
