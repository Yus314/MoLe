package net.ktnx.mobileledger.feature.transaction.usecase;

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
public final class TransactionDateNavigatorImpl_Factory implements Factory<TransactionDateNavigatorImpl> {
  @Override
  public TransactionDateNavigatorImpl get() {
    return newInstance();
  }

  public static TransactionDateNavigatorImpl_Factory create() {
    return InstanceHolder.INSTANCE;
  }

  public static TransactionDateNavigatorImpl newInstance() {
    return new TransactionDateNavigatorImpl();
  }

  private static final class InstanceHolder {
    static final TransactionDateNavigatorImpl_Factory INSTANCE = new TransactionDateNavigatorImpl_Factory();
  }
}
