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
public final class DeleteTemplateUseCaseImpl_Factory implements Factory<DeleteTemplateUseCaseImpl> {
  private final Provider<TemplateRepository> templateRepositoryProvider;

  private DeleteTemplateUseCaseImpl_Factory(
      Provider<TemplateRepository> templateRepositoryProvider) {
    this.templateRepositoryProvider = templateRepositoryProvider;
  }

  @Override
  public DeleteTemplateUseCaseImpl get() {
    return newInstance(templateRepositoryProvider.get());
  }

  public static DeleteTemplateUseCaseImpl_Factory create(
      Provider<TemplateRepository> templateRepositoryProvider) {
    return new DeleteTemplateUseCaseImpl_Factory(templateRepositoryProvider);
  }

  public static DeleteTemplateUseCaseImpl newInstance(TemplateRepository templateRepository) {
    return new DeleteTemplateUseCaseImpl(templateRepository);
  }
}
