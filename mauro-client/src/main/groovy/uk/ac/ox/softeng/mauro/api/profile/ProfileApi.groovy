package uk.ac.ox.softeng.mauro.api.profile

import uk.ac.ox.softeng.mauro.domain.facet.Metadata
import uk.ac.ox.softeng.mauro.profile.DataModelBasedProfile
import uk.ac.ox.softeng.mauro.profile.Profile
import uk.ac.ox.softeng.mauro.profile.applied.AppliedProfile
import uk.ac.ox.softeng.mauro.web.ListResponse

import groovy.transform.CompileStatic
import io.micronaut.core.annotation.Nullable
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Header
import io.micronaut.http.annotation.Post
import io.micronaut.http.client.annotation.Client

@CompileStatic
@Client('${micronaut.http.services.mauro.url}')
@Header(name='apiKey', value = '${micronaut.http.services.mauro.apikey}')
interface ProfileApi {

    @Get('/profiles/providers/dynamic')
    List<DataModelBasedProfile> dynamicProviders()

    @Get('/profiles/providers')
    List<Profile> providers()

    @Get('/profiles/{namespace}/{name}/search')
    Profile getProfileDetails(String namespace, String name)

    @Get('/{domainType}/{domainId}/profiles/{namespace}/{name}/search')
    Profile getProfileDetails(String domainType, UUID domainId, String namespace, String name)

    @Get('/profiles/providers/{namespace}/{name}/{version}')
    Profile getProfileDetails(String namespace, String name, String version)

    @Get('/{domainType}/{domainId}/profiles/used')
    List<Profile> getUsedProfiles(String domainType, UUID domainId)

    @Get('/{domainType}/{domainId}/profiles/unused')
    List<Profile> getUnusedProfiles(String domainType, UUID domainId)

    @Get('/{domainType}/{domainId}/profiles/otherMetadata')
    ListResponse<Metadata> getOtherMetadata(String domainType, UUID domainId)

    @Get('/{domainType}/{domainId}/profile/{namespace}/{name}/{version}')
    AppliedProfile getProfiledItem(String domainType, UUID domainId, String namespace, String name, String version)

    @Post('/{domainType}/{domainId}/profile/{namespace}/{name}/{version}/validate')
    AppliedProfile validateProfile(String domainType, UUID domainId, String namespace, String name, String version, @Body Map bodyMap)

    // TODO: Refactor the UI so that this method isn't needed quite so often
    @Get('/metadata/namespaces{/prefix}')
    List<MetadataNamespaceDTO> getNamespaces(@Nullable String prefix)

}
