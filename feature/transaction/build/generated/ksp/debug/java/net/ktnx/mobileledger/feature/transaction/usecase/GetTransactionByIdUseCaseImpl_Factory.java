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
public final class GetTransactionByIdUseCaseImpl_Factory implements Factory<GetTransactionByIdUseCaseImpl> {
  private final Provider<TransactionRepository> transactionRepositoryProvider;

  private GetTransactionByIdUseCaseImpl_Factory(
      Provider<TransactionRepository> transactionRepositoryProvider) {
    this.transactionRepositoryProvider = transactionRepositoryProvider;
  }

  @Override
  public GetTransactionByIdUseCaseImpl get() {
    return newInstance(transactionRepositoryProvider.get());
  }

  public static GetTransactionByIdUseCaseImpl_Factory create(
      Provider<TransactionRepository> transactionRepositoryProvider) {
    return new GetTransactionByIdUseCaseImpl_Factory(transactionRepositoryProvider);
  }

  public static GetTransactionByIdUseCaseImpl newInstance(
      TransactionRepository transactionRepository) {
    return new GetTransactionByIdUseCaseImpl(transactionRepository);
  }
}
