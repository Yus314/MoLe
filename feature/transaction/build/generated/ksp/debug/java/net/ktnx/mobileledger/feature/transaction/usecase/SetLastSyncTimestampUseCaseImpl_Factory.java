package net.ktnx.mobileledger.feature.transaction.usecase;

import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Provider;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import net.ktnx.mobileledger.core.domain.repository.OptionRepository;

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
public final class SetLastSyncTimestampUseCaseImpl_Factory implements Factory<SetLastSyncTimestampUseCaseImpl> {
  private final Provider<OptionRepository> optionRepositoryProvider;

  private SetLastSyncTimestampUseCaseImpl_Factory(
      Provider<OptionRepository> optionRepositoryProvider) {
    this.optionRepositoryProvider = optionRepositoryProvider;
  }

  @Override
  public SetLastSyncTimestampUseCaseImpl get() {
    return newInstance(optionRepositoryProvider.get());
  }

  public static SetLastSyncTimestampUseCaseImpl_Factory create(
      Provider<OptionRepository> optionRepositoryProvider) {
    return new SetLastSyncTimestampUseCaseImpl_Factory(optionRepositoryProvider);
  }

  public static SetLastSyncTimestampUseCaseImpl newInstance(OptionRepository optionRepository) {
    return new SetLastSyncTimestampUseCaseImpl(optionRepository);
  }
}
