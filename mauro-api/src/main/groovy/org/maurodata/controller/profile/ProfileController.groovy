package org.maurodata.controller.profile

import io.swagger.v3.oas.annotations.Operation
import org.maurodata.api.Paths
import org.maurodata.api.model.ModelRefDTO
import org.maurodata.api.profile.dto.AppliedProfilePayloadDTO
import org.maurodata.api.profile.dto.MetadataNamespaceDTO
import org.maurodata.api.profile.ProfileApi
import org.maurodata.api.profile.dto.MultiFacetAwareItemRefDTO
import org.maurodata.api.profile.dto.ProfileManyGetRequestDTO
import org.maurodata.api.profile.dto.ProfileManyProvidedRequestDTO
import org.maurodata.api.profile.dto.ProfileManyResponseDTO
import org.maurodata.api.profile.dto.ProfileProvidedDTO
import org.maurodata.api.profile.dto.ProfileProvidedRequestDTO
import org.maurodata.audit.Audit
import org.maurodata.controller.model.AdministeredItemReader
import org.maurodata.domain.facet.Metadata
import org.maurodata.domain.model.AdministeredItem
import org.maurodata.domain.security.Role
import org.maurodata.persistence.cache.FacetCacheableRepository.MetadataCacheableRepository
import org.maurodata.persistence.facet.MetadataRepository
import org.maurodata.persistence.model.PathRepository
import org.maurodata.persistence.profile.DynamicProfileService
import org.maurodata.plugin.MauroPluginDTO
import org.maurodata.profile.DataModelBasedProfile
import org.maurodata.profile.Profile
import org.maurodata.profile.ProfileService
import org.maurodata.profile.applied.AppliedProfile
import org.maurodata.profile.applied.AppliedProfileField
import org.maurodata.profile.applied.AppliedProfileSection
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

    @Inject
    PathRepository pathRepository

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
    @Operation(summary = "List the profiles", description = "Returns the profiles.")
    @Get(Paths.PROFILE_DYNAMIC_PROVIDERS)
    List<DataModelBasedProfile> dynamicProviders() {
        dynamicProfileService.getDynamicProfiles().collect {// Stub the datamodel for UI output
            it.dataModel.dataClasses = []
            it.dataModel.dataTypes = []
            return it
        }
    }


    @Audit
    @Operation(summary = "List the profiles", description = "Returns the profiles.")
    @Get(Paths.PROFILE_PROVIDERS)
    List<Profile> providers() {
        getAllProfiles()
    }

    @Audit
    @Operation(summary = "List the classifying profiles", description = "Returns the classifying profiles.")
    @Get(Paths.PROFILE_CLASSIFYING_PROVIDERS)
    List<Profile> classifyingProviders(@Nullable String classifierNamespace, @Nullable String classifierLabel) {
        if (classifierNamespace && classifierLabel) {
            return profileService.getClassifyingProfiles(classifierNamespace, classifierLabel)
        }
        if (classifierNamespace) {
            return profileService.getClassifyingProfiles(classifierNamespace)
        }
        profileService.getClassifyingProfiles()
    }


    @Audit
    @Operation(summary = "Get a profile", description = "Returns a profile.")
    @Get(Paths.PROFILE_SEARCH)
    Profile getProfileDetails(String namespace, String name) {
        getProfileByName(namespace, name)
    }

    @Audit
    @Operation(summary = "Get a profile", description = "Returns a profile.")
    @Get(Paths.PROFILE_SEARCH_ITEM)
    Profile getProfileDetails(String domainType, UUID domainId, String namespace, String name) {
        // TODO: I don't think this endpoint is actually used
        return null
    }

    @Audit
    @Operation(summary = "Get a profile", description = "Returns a profile.")
    @Get(Paths.PROFILE_DETAILS)
    Profile getProfileDetails(String namespace, String name, @Nullable String version) {
        getProfileByName(namespace, name, version)
    }

    @Audit
    @Operation(summary = "List the profiles", description = "Returns the profiles. You must have read privileges on the item in question.")
    @Get(Paths.PROFILE_USED)
    List<MauroPluginDTO> getUsedProfiles(String domainType, UUID domainId) {
        AdministeredItem administeredItem = findAdministeredItem(domainType, domainId)
        accessControlService.checkRole(Role.READER, administeredItem)
        profileService.getUsedProfilesForAdministeredItem(getAllProfiles(), administeredItem)
            .collect {MauroPluginDTO.fromPlugin(it) }
    }

    @Audit
    @Operation(summary = "List the profiles", description = "Returns the profiles. You must have read privileges on the item in question.")
    @Get(Paths.PROFILE_UNUSED)
    List<MauroPluginDTO> getUnusedProfiles(String domainType, UUID domainId) {
        AdministeredItem administeredItem = findAdministeredItem(domainType, domainId)
        accessControlService.checkRole(Role.READER, administeredItem)
        profileService.getUnusedProfilesForAdministeredItem(getAllProfiles(), administeredItem)
            .collect {MauroPluginDTO.fromPlugin(it) }
    }

    @Audit
    @Operation(summary = "List the profiles", description = "Returns the profiles. You must have read privileges on the item in question.")
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
    @Operation(summary = "Get a profile", description = "Returns a profile. You must have read privileges on the item in question.")
    @Get(Paths.PROFILE_ITEM)
    AppliedProfile getProfiledItem(String domainType, UUID domainId, String namespace, String name, @Nullable String version) {
        AdministeredItem administeredItem = findAdministeredItem(domainType, domainId)
        accessControlService.checkRole(Role.READER, administeredItem)
        Profile profile = getProfileByName(namespace, name, version)
        handleProfileNotFound(profile, namespace, name, version)
        new AppliedProfile(profile, administeredItem)
    }

    @Audit(level = Audit.AuditLevel.FILE_ONLY)
    @Operation(summary = "Validate the profile", description = "Validates the profile. You must have read privileges on the item in question.")
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
    @Operation(summary = "Apply the profile", description = "Applies the profile. You must have edit privileges on the item in question.")
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
    @Operation(summary = "List the profiles", description = "Returns the profiles.")
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

    private static Map<String, Object> appliedProfileSectionsToBodyMap(List<AppliedProfileSection> sections) {
        [
            sections: sections.collect {AppliedProfileSection section ->
                [
                    name: section.name,
                    label: section.label,
                    fields: section.fields.collect {AppliedProfileField field ->
                        [
                            fieldName: field.fieldName,
                            metadataPropertyName: field.metadataPropertyName,
                            currentValue: field.currentValue
                        ] as Map<String, Object>
                    }
                ] as Map<String, Object>
            }
        ] as Map<String, Object>
    }

    @Audit
    @Operation(summary = "Create a profile", description = "Creates a profile.")
    @Post(Paths.PROFILE_ITEM_GET_MANY)
    ProfileManyResponseDTO getMany(String domainType, UUID domainId, @Body ProfileManyGetRequestDTO body) {
        List<AdministeredItem> administeredItems = body.multiFacetAwareItems.collect {MultiFacetAwareItemRefDTO itemRef ->
            findAdministeredItem(itemRef.multiFacetAwareItemDomainType, itemRef.multiFacetAwareItemId)
        }
        administeredItems.each {
            pathRepository.readParentItems(it)
            it.updateBreadcrumbs()
        }
        MauroPluginDTO profileProviderService = body.profileProviderServices[0]

        Profile profile = getProfileByName(profileProviderService.namespace, profileProviderService.name, profileProviderService.version)

        new ProfileManyResponseDTO(
            count: administeredItems.size(),
            profilesProvided: administeredItems.collect {AdministeredItem administeredItem ->
                new ProfileProvidedDTO(
                    profile: new AppliedProfilePayloadDTO(new AppliedProfile(profile, administeredItem)),
                    multiFacetAwareItem: new ModelRefDTO(administeredItem),
                    profileProviderService: MauroPluginDTO.fromPlugin(profile))
            })
    }

    @Audit
    @Operation(summary = "Validate the profile", description = "Validates the profile.")
    @Post(Paths.PROFILE_ITEM_VALIDATE_MANY)
    ProfileManyResponseDTO validateMany(String domainType, UUID domainId, @Body ProfileManyProvidedRequestDTO body) {

        List<ProfileProvidedDTO> appliedProfiles = body.profilesProvided.collect {ProfileProvidedRequestDTO profileProvided ->
            Profile profile = getProfileByName(profileProvided.profileProviderService.namespace, profileProvided.profileProviderService.name, profileProvided.profileProviderService.version)
            AdministeredItem administeredItem =
                findAdministeredItem(
                    profileProvided.profile.domainType,
                    profileProvided.profile.id)
            AppliedProfile appliedProfile = new AppliedProfile(profile, administeredItem, appliedProfileSectionsToBodyMap(profileProvided.profile.sections))
            new ProfileProvidedDTO(
                profile: new AppliedProfilePayloadDTO(appliedProfile),
                multiFacetAwareItem: new ModelRefDTO(administeredItem),
                profileProviderService: MauroPluginDTO.fromPlugin(profile),
                errors: appliedProfile.errors)
        }

        new ProfileManyResponseDTO(count: appliedProfiles.size(), profilesProvided: appliedProfiles)
    }

    @Audit
    @Operation(summary = "Save the profile", description = "Saves the profile.")
    @Post(Paths.PROFILE_ITEM_SAVE_MANY)
    ProfileManyResponseDTO saveMany(String domainType, UUID domainId, @Body ProfileManyProvidedRequestDTO body) {
        List<ProfileProvidedDTO> appliedProfiles = body.profilesProvided.collect {ProfileProvidedRequestDTO profileProvided ->
            Profile profile = getProfileByName(profileProvided.profileProviderService.namespace, profileProvided.profileProviderService.name, profileProvided.profileProviderService.version)
            AdministeredItem administeredItem =
                findAdministeredItem(
                    profileProvided.profile.domainType,
                    profileProvided.profile.id)
            AppliedProfile appliedProfile = new AppliedProfile(profile, administeredItem, appliedProfileSectionsToBodyMap(profileProvided.profile.sections))

            List<Metadata> profileMetadata = appliedProfile.metadata

            // First delete the metadata saved previously for this profile
            metadataCacheableRepository.deleteAll(administeredItem.metadata.findAll {it.namespace == appliedProfile.metadataNamespace})

            // Then save the profile items as new metadata
            metadataCacheableRepository.saveAll(profileMetadata.findAll {it.value})

            new ProfileProvidedDTO(
                profile: new AppliedProfilePayloadDTO(appliedProfile),
                multiFacetAwareItem: new ModelRefDTO(administeredItem),
                profileProviderService: MauroPluginDTO.fromPlugin(profile),
                errors: appliedProfile.errors)

        }
        new ProfileManyResponseDTO(count: appliedProfiles.size(), profilesProvided: appliedProfiles)
    }



}
