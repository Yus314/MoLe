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
public final class UpdateProfileOrderUseCaseImpl_Factory implements Factory<UpdateProfileOrderUseCaseImpl> {
  private final Provider<ProfileRepository> profileRepositoryProvider;

  private UpdateProfileOrderUseCaseImpl_Factory(
      Provider<ProfileRepository> profileRepositoryProvider) {
    this.profileRepositoryProvider = profileRepositoryProvider;
  }

  @Override
  public UpdateProfileOrderUseCaseImpl get() {
    return newInstance(profileRepositoryProvider.get());
  }

  public static UpdateProfileOrderUseCaseImpl_Factory create(
      Provider<ProfileRepository> profileRepositoryProvider) {
    return new UpdateProfileOrderUseCaseImpl_Factory(profileRepositoryProvider);
  }

  public static UpdateProfileOrderUseCaseImpl newInstance(ProfileRepository profileRepository) {
    return new UpdateProfileOrderUseCaseImpl(profileRepository);
  }
}
