package net.ktnx.mobileledger.feature.transaction.usecase;

import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Provider;
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
public final class GetTransactionsUseCaseImpl_Factory implements Factory<GetTransactionsUseCaseImpl> {
  private final Provider<ObserveTransactionsUseCase> observeTransactionsUseCaseProvider;

  private GetTransactionsUseCaseImpl_Factory(
      Provider<ObserveTransactionsUseCase> observeTransactionsUseCaseProvider) {
    this.observeTransactionsUseCaseProvider = observeTransactionsUseCaseProvider;
  }

  @Override
  public GetTransactionsUseCaseImpl get() {
    return newInstance(observeTransactionsUseCaseProvider.get());
  }

  public static GetTransactionsUseCaseImpl_Factory create(
      Provider<ObserveTransactionsUseCase> observeTransactionsUseCaseProvider) {
    return new GetTransactionsUseCaseImpl_Factory(observeTransactionsUseCaseProvider);
  }

  public static GetTransactionsUseCaseImpl newInstance(
      ObserveTransactionsUseCase observeTransactionsUseCase) {
    return new GetTransactionsUseCaseImpl(observeTransactionsUseCase);
  }
}
