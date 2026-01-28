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
public final class GetAllTemplatesUseCaseImpl_Factory implements Factory<GetAllTemplatesUseCaseImpl> {
  private final Provider<TemplateRepository> templateRepositoryProvider;

  private GetAllTemplatesUseCaseImpl_Factory(
      Provider<TemplateRepository> templateRepositoryProvider) {
    this.templateRepositoryProvider = templateRepositoryProvider;
  }

  @Override
  public GetAllTemplatesUseCaseImpl get() {
    return newInstance(templateRepositoryProvider.get());
  }

  public static GetAllTemplatesUseCaseImpl_Factory create(
      Provider<TemplateRepository> templateRepositoryProvider) {
    return new GetAllTemplatesUseCaseImpl_Factory(templateRepositoryProvider);
  }

  public static GetAllTemplatesUseCaseImpl newInstance(TemplateRepository templateRepository) {
    return new GetAllTemplatesUseCaseImpl(templateRepository);
  }
}
