package uk.ac.ox.softeng.mauro.controller.profile

import uk.ac.ox.softeng.mauro.api.Paths
import uk.ac.ox.softeng.mauro.api.profile.MetadataNamespaceDTO
import uk.ac.ox.softeng.mauro.api.profile.ProfileApi
import uk.ac.ox.softeng.mauro.domain.security.Role
import uk.ac.ox.softeng.mauro.plugin.MauroPluginDTO
import uk.ac.ox.softeng.mauro.profile.applied.AppliedProfile

import io.micronaut.core.annotation.Nullable
import io.micronaut.http.HttpStatus
import io.micronaut.http.annotation.Body
import io.micronaut.http.exceptions.HttpStatusException
import io.micronaut.security.annotation.Secured
import io.micronaut.security.rules.SecurityRule
import uk.ac.ox.softeng.mauro.controller.model.AdministeredItemReader
import uk.ac.ox.softeng.mauro.domain.facet.Metadata
import uk.ac.ox.softeng.mauro.domain.model.AdministeredItem

import groovy.transform.CompileStatic
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Post
import jakarta.inject.Inject
import uk.ac.ox.softeng.mauro.persistence.facet.MetadataRepository
import uk.ac.ox.softeng.mauro.persistence.profile.DynamicProfileService
import uk.ac.ox.softeng.mauro.profile.DataModelBasedProfile
import uk.ac.ox.softeng.mauro.profile.Profile
import uk.ac.ox.softeng.mauro.profile.ProfileService
import uk.ac.ox.softeng.mauro.security.AccessControlService
import uk.ac.ox.softeng.mauro.web.ListResponse

@CompileStatic
@Controller
@Secured(SecurityRule.IS_ANONYMOUS)
class ProfileController implements AdministeredItemReader, ProfileApi {

    @Inject
    AccessControlService accessControlService

    @Inject
    ProfileService profileService

    @Inject
    DynamicProfileService dynamicProfileService

    @Inject
    MetadataRepository metadataRepository

    ProfileController() {}

    private List<Profile> getAllProfiles() {
        profileService.getStaticProfiles() + dynamicProfileService.getDynamicProfiles().collect {(Profile) it}
    }

    private Profile getProfileByName(String namespace, String name, String version = null) {
        getAllProfiles().find {
            it.namespace == namespace &&
                    it.name == name &&
                    (!version || it.version == version)
        }
    }


    @Get(Paths.PROFILE_DYNAMIC_PROVIDERS)
    List<DataModelBasedProfile> dynamicProviders() {
        dynamicProfileService.getDynamicProfiles()
    }


    @Get(Paths.PROFILE_PROVIDERS)
    List<Profile> providers() {
        getAllProfiles()
    }


    @Get(Paths.PROFILE_SEARCH)
    Profile getProfileDetails(String namespace, String name) {
        // TODO: I don't think this endpoint is actually used
        return null
    }

    @Get(Paths.PROFILE_SEARCH_ITEM)
    Profile getProfileDetails(String domainType, UUID domainId, String namespace, String name) {
        // TODO: I don't think this endpoint is actually used
        return null
    }


    @Get(Paths.PROFILE_DETAILS)
    Profile getProfileDetails(String namespace, String name, String version) {
        getProfileByName(namespace, name, version)
    }

    @Get(Paths.PROFILE_USED)
    List<MauroPluginDTO> getUsedProfiles(String domainType, UUID domainId) {
        AdministeredItem administeredItem = findAdministeredItem(domainType, domainId)
        accessControlService.checkRole(Role.READER, administeredItem)
        profileService.getUsedProfilesForAdministeredItem(getAllProfiles(), administeredItem)
            .collect {MauroPluginDTO.fromPlugin(it) }
    }

    @Get(Paths.PROFILE_UNUSED)
    List<MauroPluginDTO> getUnusedProfiles(String domainType, UUID domainId) {
        AdministeredItem administeredItem = findAdministeredItem(domainType, domainId)
        accessControlService.checkRole(Role.READER, administeredItem)
        profileService.getUnusedProfilesForAdministeredItem(getAllProfiles(), administeredItem)
            .collect {MauroPluginDTO.fromPlugin(it) }
    }

    @Get(Paths.PROFILE_OTHER_METADATA)
    ListResponse<Metadata> getOtherMetadata(String domainType, UUID domainId) {
        AdministeredItem administeredItem = findAdministeredItem(domainType, domainId)
        accessControlService.checkRole(Role.READER, administeredItem)
        List<Profile> usedProfiles = profileService.getUsedProfilesForAdministeredItem(getAllProfiles(), administeredItem)
        List<String> usedProfileNamespaces = usedProfiles.namespace
        ListResponse.from(administeredItem.metadata.findAll { md ->
            !usedProfileNamespaces.contains(md.namespace)
        })
    }

    @Get(Paths.PROFILE_ITEM)
    AppliedProfile getProfiledItem(String domainType, UUID domainId, String namespace, String name, String version) {
        AdministeredItem administeredItem = findAdministeredItem(domainType, domainId)
        accessControlService.checkRole(Role.READER, administeredItem)
        Profile profile = getProfileByName(namespace, name, version)
        handleProfileNotFound(profile, namespace, name, version)
        def ap = new AppliedProfile(profile, administeredItem)
        return ap
    }

    @Post(Paths.PROFILE_ITEM_VALIDATE)
    AppliedProfile validateProfile(String domainType, UUID domainId, String namespace, String name, String version, @Body Map bodyMap) {
        AdministeredItem administeredItem = readAdministeredItem(domainType, domainId)
        accessControlService.canDoRole(Role.READER, administeredItem)
        Profile profile = getProfileByName(namespace, name, version)
        handleProfileNotFound(profile, namespace, name, version)
        // Overwrite applied profile with metadata items from the bodyMap
        new AppliedProfile(profile, administeredItem, bodyMap)
    }


    // TODO: Refactor the UI so that this method isn't needed quite so often
    @Get(Paths.PROFILE_NAMESPACES)
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

    static void handleProfileNotFound(Profile profile, String namespace, String name, String version) {
        if (!profile) {
            throw new HttpStatusException(HttpStatus.BAD_REQUEST, "Profile with namespace: ${namespace}, name: ${name} and version: ${version} not found")
        }
    }

}
