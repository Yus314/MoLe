package net.ktnx.mobileledger.feature.account.usecase;

import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
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
public final class AccountHierarchyResolverImpl_Factory implements Factory<AccountHierarchyResolverImpl> {
  @Override
  public AccountHierarchyResolverImpl get() {
    return newInstance();
  }

  public static AccountHierarchyResolverImpl_Factory create() {
    return InstanceHolder.INSTANCE;
  }

  public static AccountHierarchyResolverImpl newInstance() {
    return new AccountHierarchyResolverImpl();
  }

  private static final class InstanceHolder {
    static final AccountHierarchyResolverImpl_Factory INSTANCE = new AccountHierarchyResolverImpl_Factory();
  }
}
