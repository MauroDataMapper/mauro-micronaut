package uk.ac.ox.softeng.mauro.api.profile

import uk.ac.ox.softeng.mauro.api.MauroApi
import uk.ac.ox.softeng.mauro.api.Paths
import uk.ac.ox.softeng.mauro.domain.facet.Metadata
import uk.ac.ox.softeng.mauro.profile.DataModelBasedProfile
import uk.ac.ox.softeng.mauro.profile.Profile
import uk.ac.ox.softeng.mauro.profile.applied.AppliedProfile
import uk.ac.ox.softeng.mauro.web.ListResponse

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
    List<Profile> getUsedProfiles(String domainType, UUID domainId)

    @Get(Paths.PROFILE_UNUSED)
    List<Profile> getUnusedProfiles(String domainType, UUID domainId)

    @Get(Paths.PROFILE_OTHER_METADATA)
    ListResponse<Metadata> getOtherMetadata(String domainType, UUID domainId)

    @Get(Paths.PROFILE_ITEM)
    AppliedProfile getProfiledItem(String domainType, UUID domainId, String namespace, String name, String version)

    @Post(Paths.PROFILE_ITEM_VALIDATE)
    AppliedProfile validateProfile(String domainType, UUID domainId, String namespace, String name, String version, @Body Map bodyMap)

    // TODO: Refactor the UI so that this method isn't needed quite so often
    @Get(Paths.PROFILE_NAMESPACES)
    List<MetadataNamespaceDTO> getNamespaces(@Nullable String prefix)

}
