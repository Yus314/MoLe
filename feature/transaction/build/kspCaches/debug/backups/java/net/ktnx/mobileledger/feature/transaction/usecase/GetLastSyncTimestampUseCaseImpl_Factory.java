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
public final class GetLastSyncTimestampUseCaseImpl_Factory implements Factory<GetLastSyncTimestampUseCaseImpl> {
  private final Provider<OptionRepository> optionRepositoryProvider;

  private GetLastSyncTimestampUseCaseImpl_Factory(
      Provider<OptionRepository> optionRepositoryProvider) {
    this.optionRepositoryProvider = optionRepositoryProvider;
  }

  @Override
  public GetLastSyncTimestampUseCaseImpl get() {
    return newInstance(optionRepositoryProvider.get());
  }

  public static GetLastSyncTimestampUseCaseImpl_Factory create(
      Provider<OptionRepository> optionRepositoryProvider) {
    return new GetLastSyncTimestampUseCaseImpl_Factory(optionRepositoryProvider);
  }

  public static GetLastSyncTimestampUseCaseImpl newInstance(OptionRepository optionRepository) {
    return new GetLastSyncTimestampUseCaseImpl(optionRepository);
  }
}
