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
public final class SearchTransactionDescriptionsUseCaseImpl_Factory implements Factory<SearchTransactionDescriptionsUseCaseImpl> {
  private final Provider<TransactionRepository> transactionRepositoryProvider;

  private SearchTransactionDescriptionsUseCaseImpl_Factory(
      Provider<TransactionRepository> transactionRepositoryProvider) {
    this.transactionRepositoryProvider = transactionRepositoryProvider;
  }

  @Override
  public SearchTransactionDescriptionsUseCaseImpl get() {
    return newInstance(transactionRepositoryProvider.get());
  }

  public static SearchTransactionDescriptionsUseCaseImpl_Factory create(
      Provider<TransactionRepository> transactionRepositoryProvider) {
    return new SearchTransactionDescriptionsUseCaseImpl_Factory(transactionRepositoryProvider);
  }

  public static SearchTransactionDescriptionsUseCaseImpl newInstance(
      TransactionRepository transactionRepository) {
    return new SearchTransactionDescriptionsUseCaseImpl(transactionRepository);
  }
}
