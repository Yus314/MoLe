package net.ktnx.mobileledger.feature.account.usecase;

import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Provider;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import net.ktnx.mobileledger.core.domain.repository.PreferencesRepository;

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
public final class SetShowZeroBalanceUseCaseImpl_Factory implements Factory<SetShowZeroBalanceUseCaseImpl> {
  private final Provider<PreferencesRepository> preferencesRepositoryProvider;

  private SetShowZeroBalanceUseCaseImpl_Factory(
      Provider<PreferencesRepository> preferencesRepositoryProvider) {
    this.preferencesRepositoryProvider = preferencesRepositoryProvider;
  }

  @Override
  public SetShowZeroBalanceUseCaseImpl get() {
    return newInstance(preferencesRepositoryProvider.get());
  }

  public static SetShowZeroBalanceUseCaseImpl_Factory create(
      Provider<PreferencesRepository> preferencesRepositoryProvider) {
    return new SetShowZeroBalanceUseCaseImpl_Factory(preferencesRepositoryProvider);
  }

  public static SetShowZeroBalanceUseCaseImpl newInstance(
      PreferencesRepository preferencesRepository) {
    return new SetShowZeroBalanceUseCaseImpl(preferencesRepository);
  }
}
