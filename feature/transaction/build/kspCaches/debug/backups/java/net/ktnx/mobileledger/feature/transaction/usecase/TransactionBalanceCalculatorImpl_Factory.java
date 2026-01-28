package net.ktnx.mobileledger.feature.transaction.usecase;

import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;

@ScopeMetadata("javax.inject.Singleton")
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
public final class TransactionBalanceCalculatorImpl_Factory implements Factory<TransactionBalanceCalculatorImpl> {
  @Override
  public TransactionBalanceCalculatorImpl get() {
    return newInstance();
  }

  public static TransactionBalanceCalculatorImpl_Factory create() {
    return InstanceHolder.INSTANCE;
  }

  public static TransactionBalanceCalculatorImpl newInstance() {
    return new TransactionBalanceCalculatorImpl();
  }

  private static final class InstanceHolder {
    static final TransactionBalanceCalculatorImpl_Factory INSTANCE = new TransactionBalanceCalculatorImpl_Factory();
  }
}
