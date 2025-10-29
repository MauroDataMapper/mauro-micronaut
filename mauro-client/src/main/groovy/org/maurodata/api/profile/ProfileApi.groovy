package org.maurodata.api.profile

import io.micronaut.core.annotation.NonNull
import org.maurodata.api.MauroApi
import org.maurodata.api.Paths
import org.maurodata.domain.facet.Metadata
import org.maurodata.plugin.MauroPluginDTO
import org.maurodata.profile.DataModelBasedProfile
import org.maurodata.profile.Profile
import org.maurodata.profile.ProfilesProvidedDTO
import org.maurodata.profile.applied.AppliedProfile
import org.maurodata.web.ListResponse

import io.micronaut.core.annotation.Nullable
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Post

@MauroApi
interface ProfileApi {

    @Get(Paths.PROFILE_DYNAMIC_PROVIDERS)
    List<DataModelBasedProfile> dynamicProviders()

    @Get(Paths.PROFILE_PROVIDERS)
    List<Profile> providers()

    @Get(Paths.PROFILE_SEARCH)
    Profile getProfileDetails(String namespace, String name)

    @Get(Paths.PROFILE_SEARCH_ITEM)
    Profile getProfileDetails(String domainType, UUID domainId, String namespace, String name)

    @Get(Paths.PROFILE_DETAILS)
    Profile getProfileDetails(String namespace, String name, String version)

    @Get(Paths.PROFILE_USED)
    List<MauroPluginDTO> getUsedProfiles(String domainType, UUID domainId)

    @Get(Paths.PROFILE_UNUSED)
    List<MauroPluginDTO> getUnusedProfiles(String domainType, UUID domainId)

    @Get(Paths.PROFILE_OTHER_METADATA)
    ListResponse<Metadata> getOtherMetadata(String domainType, UUID domainId)

    @Get(Paths.PROFILE_ITEM)
    AppliedProfile getProfiledItem(String domainType, UUID domainId, String namespace, String name, String version)

    @Post(Paths.PROFILE_ITEM_VALIDATE)
    AppliedProfile validateProfile(String domainType, UUID domainId, String namespace, String name, String version, @Body Map bodyMap)

    // TODO: Refactor the UI so that this method isn't needed quite so often
    @Get(Paths.PROFILE_NAMESPACES)
    List<MetadataNamespaceDTO> getNamespaces(@Nullable String prefix)

    @Post(Paths.PROFILE_GET_MANY)
    ProfilesProvidedDTO getMany(@NonNull String domainType, @NonNull UUID domainId, @Body Map bodyMap)

    @Post(Paths.PROFILE_VALIDATE_MANY)
    ProfilesProvidedDTO validateMany(@NonNull String domainType, @NonNull UUID domainId, @Body ProfilesProvidedDTO profilesProvidedDTO)
}
