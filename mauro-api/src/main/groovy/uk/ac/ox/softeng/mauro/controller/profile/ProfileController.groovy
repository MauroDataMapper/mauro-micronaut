package uk.ac.ox.softeng.mauro.controller.profile

import io.micronaut.core.annotation.Nullable
import io.micronaut.security.annotation.Secured
import io.micronaut.security.rules.SecurityRule
import uk.ac.ox.softeng.mauro.controller.model.AdministeredItemReader
import uk.ac.ox.softeng.mauro.domain.facet.Metadata
import uk.ac.ox.softeng.mauro.domain.model.AdministeredItem

import groovy.transform.CompileStatic
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import jakarta.inject.Inject
import uk.ac.ox.softeng.mauro.persistence.facet.MetadataRepository
import uk.ac.ox.softeng.mauro.persistence.profile.DynamicProfileService
import uk.ac.ox.softeng.mauro.profile.DataModelBasedProfile
import uk.ac.ox.softeng.mauro.profile.Profile
import uk.ac.ox.softeng.mauro.profile.ProfileService
import uk.ac.ox.softeng.mauro.web.ListResponse

@CompileStatic
@Controller
@Secured(SecurityRule.IS_ANONYMOUS)
class ProfileController implements AdministeredItemReader {

    @Inject
    ProfileService profileService

    @Inject
    DynamicProfileService dynamicProfileService

    @Inject
    MetadataRepository metadataRepository

    private List<Profile> getAllProfiles() {
        profileService.getStaticProfiles() + dynamicProfileService.getDynamicProfiles().collect {(Profile) it}
    }

    @Get('/profiles/providers/dynamic')
    List<DataModelBasedProfile> dynamicProviders() {
        dynamicProfileService.getDynamicProfiles()
    }


    @Get('/profiles/providers')
    List<Profile> providers() {
        getAllProfiles()
    }


    @Get('/profiles/{namespace}/{name}/search')
    Profile getProfileDetails(String namespace, String name) {
        // TODO: I don't think this endpoint is actually used
        return null
    }

    @Get('/{domainType}/{domainId}/profiles/{namespace}/{name}/search')
    Profile getProfileDetails(String domainType, UUID domainId, String namespace, String name) {
        // TODO: I don't think this endpoint is actually used
        return null
    }


    @Get('/profiles/providers/{namespace}/{name}/{version}')
    Profile getProfileDetails(String namespace, String name, String version) {
        getAllProfiles().find {
                    it.namespace == namespace &&
                    it.name == name &&
                    it.version == version
        }
    }

    @Get('/{domainType}/{domainId}/profiles/used')
    List<Profile> getUsedProfiles(String domainType, UUID domainId) {
        AdministeredItem administeredItem = findAdministeredItem(domainType, domainId)
        profileService.getUsedProfilesForAdministeredItem(getAllProfiles(), administeredItem)
    }

    @Get('/{domainType}/{domainId}/profiles/unused')
    List<Profile> getUnusedProfiles(String domainType, UUID domainId) {
        AdministeredItem administeredItem = findAdministeredItem(domainType, domainId)
        profileService.getUnusedProfilesForAdministeredItem(getAllProfiles(), administeredItem)
    }

    @Get('/{domainType}/{domainId}/profiles/otherMetadata')
    ListResponse<Metadata> getOtherMetadata(String domainType, UUID domainId) {
        AdministeredItem administeredItem = readAdministeredItem(domainType, domainId)
        List<Profile> usedProfiles = profileService.getUsedProfilesForAdministeredItem(getAllProfiles(), administeredItem)
        List<String> usedProfileNamespaces = usedProfiles.namespace
        ListResponse.from(administeredItem.metadata.findAll { md ->
            !usedProfileNamespaces.contains(md.namespace)
        })
    }

    // TODO: Refactor the UI so that this method isn't needed quite so often
    @Get('/metadata/namespaces{/prefix}')
    List<MetadataNamespaceDTO> getNamespaces(@Nullable String prefix) {

        // First look through the database to find all the namespaces / keys in use
        Map<String, Set<String>> namespacesAsMap = metadataRepository.getNamespaceKeys()

        // Then add all those taken from profiles
        getAllProfiles().each { profile ->
            Set<String> keys = namespacesAsMap.get(profile.metadataNamespace, [] as Set)
            keys.addAll(profile.getKeys())
            namespacesAsMap[profile.metadataNamespace] = keys
        }

        namespacesAsMap
                .findAll {!prefix || it.key.startsWith(prefix)}
                .collect { namespace, keys ->
                        new MetadataNamespaceDTO(
                                namespace: namespace,
                                editable: false,
                                defaultNamespace: false,
                                keys: keys.sort()
                        )
                }
    }

}
