package net.ktnx.mobileledger.feature.transaction.usecase;

import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Provider;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import net.ktnx.mobileledger.core.domain.repository.TransactionRepository;

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
public final class GetFirstTransactionByDescriptionUseCaseImpl_Factory implements Factory<GetFirstTransactionByDescriptionUseCaseImpl> {
  private final Provider<TransactionRepository> transactionRepositoryProvider;

  private GetFirstTransactionByDescriptionUseCaseImpl_Factory(
      Provider<TransactionRepository> transactionRepositoryProvider) {
    this.transactionRepositoryProvider = transactionRepositoryProvider;
  }

  @Override
  public GetFirstTransactionByDescriptionUseCaseImpl get() {
    return newInstance(transactionRepositoryProvider.get());
  }

  public static GetFirstTransactionByDescriptionUseCaseImpl_Factory create(
      Provider<TransactionRepository> transactionRepositoryProvider) {
    return new GetFirstTransactionByDescriptionUseCaseImpl_Factory(transactionRepositoryProvider);
  }

  public static GetFirstTransactionByDescriptionUseCaseImpl newInstance(
      TransactionRepository transactionRepository) {
    return new GetFirstTransactionByDescriptionUseCaseImpl(transactionRepository);
  }
}
