package net.ktnx.mobileledger.feature.transaction.usecase;

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
public final class SearchAccountNamesUseCaseImpl_Factory implements Factory<SearchAccountNamesUseCaseImpl> {
  private final Provider<AccountRepository> accountRepositoryProvider;

  private SearchAccountNamesUseCaseImpl_Factory(
      Provider<AccountRepository> accountRepositoryProvider) {
    this.accountRepositoryProvider = accountRepositoryProvider;
  }

  @Override
  public SearchAccountNamesUseCaseImpl get() {
    return newInstance(accountRepositoryProvider.get());
  }

  public static SearchAccountNamesUseCaseImpl_Factory create(
      Provider<AccountRepository> accountRepositoryProvider) {
    return new SearchAccountNamesUseCaseImpl_Factory(accountRepositoryProvider);
  }

  public static SearchAccountNamesUseCaseImpl newInstance(AccountRepository accountRepository) {
    return new SearchAccountNamesUseCaseImpl(accountRepository);
  }
}
