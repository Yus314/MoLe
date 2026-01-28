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
public final class GetAllProfilesUseCaseImpl_Factory implements Factory<GetAllProfilesUseCaseImpl> {
  private final Provider<ProfileRepository> profileRepositoryProvider;

  private GetAllProfilesUseCaseImpl_Factory(Provider<ProfileRepository> profileRepositoryProvider) {
    this.profileRepositoryProvider = profileRepositoryProvider;
  }

  @Override
  public GetAllProfilesUseCaseImpl get() {
    return newInstance(profileRepositoryProvider.get());
  }

  public static GetAllProfilesUseCaseImpl_Factory create(
      Provider<ProfileRepository> profileRepositoryProvider) {
    return new GetAllProfilesUseCaseImpl_Factory(profileRepositoryProvider);
  }

  public static GetAllProfilesUseCaseImpl newInstance(ProfileRepository profileRepository) {
    return new GetAllProfilesUseCaseImpl(profileRepository);
  }
}
