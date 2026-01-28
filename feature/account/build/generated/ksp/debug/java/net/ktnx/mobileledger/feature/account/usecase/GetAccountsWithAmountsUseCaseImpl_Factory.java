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
public final class GetAccountsWithAmountsUseCaseImpl_Factory implements Factory<GetAccountsWithAmountsUseCaseImpl> {
  private final Provider<AccountRepository> accountRepositoryProvider;

  private GetAccountsWithAmountsUseCaseImpl_Factory(
      Provider<AccountRepository> accountRepositoryProvider) {
    this.accountRepositoryProvider = accountRepositoryProvider;
  }

  @Override
  public GetAccountsWithAmountsUseCaseImpl get() {
    return newInstance(accountRepositoryProvider.get());
  }

  public static GetAccountsWithAmountsUseCaseImpl_Factory create(
      Provider<AccountRepository> accountRepositoryProvider) {
    return new GetAccountsWithAmountsUseCaseImpl_Factory(accountRepositoryProvider);
  }

  public static GetAccountsWithAmountsUseCaseImpl newInstance(AccountRepository accountRepository) {
    return new GetAccountsWithAmountsUseCaseImpl(accountRepository);
  }
}
