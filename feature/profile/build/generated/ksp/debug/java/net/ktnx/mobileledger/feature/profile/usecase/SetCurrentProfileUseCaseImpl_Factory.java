package net.ktnx.mobileledger.feature.profile.usecase;

import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Provider;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import net.ktnx.mobileledger.core.domain.repository.ProfileRepository;

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
public final class SetCurrentProfileUseCaseImpl_Factory implements Factory<SetCurrentProfileUseCaseImpl> {
  private final Provider<ProfileRepository> profileRepositoryProvider;

  private SetCurrentProfileUseCaseImpl_Factory(
      Provider<ProfileRepository> profileRepositoryProvider) {
    this.profileRepositoryProvider = profileRepositoryProvider;
  }

  @Override
  public SetCurrentProfileUseCaseImpl get() {
    return newInstance(profileRepositoryProvider.get());
  }

  public static SetCurrentProfileUseCaseImpl_Factory create(
      Provider<ProfileRepository> profileRepositoryProvider) {
    return new SetCurrentProfileUseCaseImpl_Factory(profileRepositoryProvider);
  }

  public static SetCurrentProfileUseCaseImpl newInstance(ProfileRepository profileRepository) {
    return new SetCurrentProfileUseCaseImpl(profileRepository);
  }
}
