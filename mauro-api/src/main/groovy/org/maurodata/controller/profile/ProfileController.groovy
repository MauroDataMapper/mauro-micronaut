package org.maurodata.controller.profile

import org.maurodata.api.Paths
import org.maurodata.api.profile.MetadataNamespaceDTO
import org.maurodata.api.profile.ProfileApi
import org.maurodata.audit.Audit
import org.maurodata.controller.model.AdministeredItemReader
import org.maurodata.domain.facet.Metadata
import org.maurodata.domain.model.AdministeredItem
import org.maurodata.domain.security.Role
import org.maurodata.persistence.cache.FacetCacheableRepository.MetadataCacheableRepository
import org.maurodata.persistence.facet.MetadataRepository
import org.maurodata.persistence.profile.DynamicProfileService
import org.maurodata.plugin.MauroPluginDTO
import org.maurodata.profile.DataModelBasedProfile
import org.maurodata.profile.Profile
import org.maurodata.profile.ProfileService
import org.maurodata.profile.applied.AppliedProfile
import org.maurodata.security.AccessControlService
import org.maurodata.web.ListResponse

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import io.micronaut.core.annotation.Nullable
import io.micronaut.http.HttpStatus
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Post
import io.micronaut.http.exceptions.HttpStatusException
import io.micronaut.security.annotation.Secured
import io.micronaut.security.rules.SecurityRule
import jakarta.inject.Inject

@CompileStatic
@Controller
@Secured(SecurityRule.IS_ANONYMOUS)
@Slf4j
class ProfileController implements AdministeredItemReader, ProfileApi {

    @Inject
    AccessControlService accessControlService

    @Inject
    ProfileService profileService

    @Inject
    DynamicProfileService dynamicProfileService

    @Inject
    MetadataRepository metadataRepository

    @Inject
    MetadataCacheableRepository metadataCacheableRepository

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


    @Audit
    @Get(Paths.PROFILE_DYNAMIC_PROVIDERS)
    List<DataModelBasedProfile> dynamicProviders() {
        dynamicProfileService.getDynamicProfiles()
    }


    @Audit
    @Get(Paths.PROFILE_PROVIDERS)
    List<Profile> providers() {
        getAllProfiles()
    }


    @Audit
    @Get(Paths.PROFILE_SEARCH)
    Profile getProfileDetails(String namespace, String name) {
        getProfileByName(namespace, name)
    }

    @Audit
    @Get(Paths.PROFILE_SEARCH_ITEM)
    Profile getProfileDetails(String domainType, UUID domainId, String namespace, String name) {
        // TODO: I don't think this endpoint is actually used
        return null
    }

    @Audit
    @Get(Paths.PROFILE_DETAILS)
    Profile getProfileDetails(String namespace, String name, @Nullable String version) {
        getProfileByName(namespace, name, version)
    }

    @Audit
    @Get(Paths.PROFILE_USED)
    List<MauroPluginDTO> getUsedProfiles(String domainType, UUID domainId) {
        AdministeredItem administeredItem = findAdministeredItem(domainType, domainId)
        accessControlService.checkRole(Role.READER, administeredItem)
        profileService.getUsedProfilesForAdministeredItem(getAllProfiles(), administeredItem)
            .collect {MauroPluginDTO.fromPlugin(it) }
    }

    @Audit
    @Get(Paths.PROFILE_UNUSED)
    List<MauroPluginDTO> getUnusedProfiles(String domainType, UUID domainId) {
        AdministeredItem administeredItem = findAdministeredItem(domainType, domainId)
        accessControlService.checkRole(Role.READER, administeredItem)
        profileService.getUnusedProfilesForAdministeredItem(getAllProfiles(), administeredItem)
            .collect {MauroPluginDTO.fromPlugin(it) }
    }

    @Audit
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

    @Audit
    @Get(Paths.PROFILE_ITEM)
    AppliedProfile getProfiledItem(String domainType, UUID domainId, String namespace, String name, @Nullable String version) {
        AdministeredItem administeredItem = findAdministeredItem(domainType, domainId)
        accessControlService.checkRole(Role.READER, administeredItem)
        Profile profile = getProfileByName(namespace, name, version)
        handleProfileNotFound(profile, namespace, name, version)
        new AppliedProfile(profile, administeredItem)
    }

    @Audit(level = Audit.AuditLevel.FILE_ONLY)
    @Post(Paths.PROFILE_ITEM_VALIDATE)
    AppliedProfile validateProfile(String domainType, UUID domainId, String namespace, String name, @Nullable String version, @Body Map bodyMap) {
        AdministeredItem administeredItem = readAdministeredItem(domainType, domainId)
        accessControlService.canDoRole(Role.READER, administeredItem)
        Profile profile = getProfileByName(namespace, name, version)
        handleProfileNotFound(profile, namespace, name, version)
        // Overwrite applied profile with metadata items from the bodyMap
        new AppliedProfile(profile, administeredItem, bodyMap)
    }

    @Audit
    @Post(Paths.PROFILE_ITEM)
    AppliedProfile applyProfile(String domainType, UUID domainId, String namespace, String name, @Nullable String version, @Body Map bodyMap) {
        AdministeredItem administeredItem = findAdministeredItem(domainType, domainId)
        accessControlService.canDoRole(Role.EDITOR, administeredItem)
        Profile profile = getProfileByName(namespace, name, version)
        handleProfileNotFound(profile, namespace, name, version)
        // Overwrite applied profile with metadata items from the bodyMap
        AppliedProfile appliedProfile = new AppliedProfile(profile, administeredItem, bodyMap)
        List<Metadata> profileMetadata = appliedProfile.metadata

        // First delete the metadata saved previously for this profile
        metadataCacheableRepository.deleteAll(administeredItem.metadata.findAll {it.namespace == appliedProfile.metadataNamespace})

        // Then save the profile items as new metadata
        metadataCacheableRepository.saveAll(profileMetadata.findAll {it.value})
        appliedProfile
    }

    // TODO: Refactor the UI so that this method isn't needed quite so often
    @Audit
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
            throw new HttpStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Profile with namespace: ${namespace}, name: ${name} and version: ${version} not found")
        }
    }

}
