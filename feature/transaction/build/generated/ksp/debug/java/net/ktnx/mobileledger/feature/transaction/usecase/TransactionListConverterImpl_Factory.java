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
public final class TransactionListConverterImpl_Factory implements Factory<TransactionListConverterImpl> {
  @Override
  public TransactionListConverterImpl get() {
    return newInstance();
  }

  public static TransactionListConverterImpl_Factory create() {
    return InstanceHolder.INSTANCE;
  }

  public static TransactionListConverterImpl newInstance() {
    return new TransactionListConverterImpl();
  }

  private static final class InstanceHolder {
    static final TransactionListConverterImpl_Factory INSTANCE = new TransactionListConverterImpl_Factory();
  }
}
