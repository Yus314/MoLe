package net.ktnx.mobileledger.feature.account.usecase;

import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Provider;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import net.ktnx.mobileledger.core.domain.repository.AccountRepository;

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
public final class ObserveAccountsWithAmountsUseCaseImpl_Factory implements Factory<ObserveAccountsWithAmountsUseCaseImpl> {
  private final Provider<AccountRepository> accountRepositoryProvider;

  private ObserveAccountsWithAmountsUseCaseImpl_Factory(
      Provider<AccountRepository> accountRepositoryProvider) {
    this.accountRepositoryProvider = accountRepositoryProvider;
  }

  @Override
  public ObserveAccountsWithAmountsUseCaseImpl get() {
    return newInstance(accountRepositoryProvider.get());
  }

  public static ObserveAccountsWithAmountsUseCaseImpl_Factory create(
      Provider<AccountRepository> accountRepositoryProvider) {
    return new ObserveAccountsWithAmountsUseCaseImpl_Factory(accountRepositoryProvider);
  }

  public static ObserveAccountsWithAmountsUseCaseImpl newInstance(
      AccountRepository accountRepository) {
    return new ObserveAccountsWithAmountsUseCaseImpl(accountRepository);
  }
}
