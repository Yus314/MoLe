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
public final class ObserveTransactionsUseCaseImpl_Factory implements Factory<ObserveTransactionsUseCaseImpl> {
  private final Provider<TransactionRepository> transactionRepositoryProvider;

  private ObserveTransactionsUseCaseImpl_Factory(
      Provider<TransactionRepository> transactionRepositoryProvider) {
    this.transactionRepositoryProvider = transactionRepositoryProvider;
  }

  @Override
  public ObserveTransactionsUseCaseImpl get() {
    return newInstance(transactionRepositoryProvider.get());
  }

  public static ObserveTransactionsUseCaseImpl_Factory create(
      Provider<TransactionRepository> transactionRepositoryProvider) {
    return new ObserveTransactionsUseCaseImpl_Factory(transactionRepositoryProvider);
  }

  public static ObserveTransactionsUseCaseImpl newInstance(
      TransactionRepository transactionRepository) {
    return new ObserveTransactionsUseCaseImpl(transactionRepository);
  }
}
