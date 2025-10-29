package org.maurodata.controller.profile

import com.fasterxml.jackson.annotation.JsonProperty
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import io.micronaut.core.annotation.NonNull
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
import org.maurodata.ErrorHandler
import org.maurodata.api.Paths
import org.maurodata.api.profile.MetadataNamespaceDTO
import org.maurodata.api.profile.ProfileApi
import org.maurodata.audit.Audit
import org.maurodata.controller.model.AdministeredItemReader
import org.maurodata.domain.facet.Metadata
import org.maurodata.domain.model.AdministeredItem
import org.maurodata.domain.model.Model
import org.maurodata.domain.security.Role
import org.maurodata.persistence.cache.FacetCacheableRepository.MetadataCacheableRepository
import org.maurodata.persistence.facet.MetadataRepository
import org.maurodata.persistence.model.PathRepository
import org.maurodata.persistence.profile.DynamicProfileService
import org.maurodata.plugin.MauroPluginDTO
import org.maurodata.profile.DataModelBasedProfile
import org.maurodata.profile.Profile
import org.maurodata.profile.ProfileProvided
import org.maurodata.profile.ProfileService
import org.maurodata.profile.ProfilesProvidedDTO
import org.maurodata.profile.applied.AppliedProfile
import org.maurodata.security.AccessControlService
import org.maurodata.web.ListResponse

@CompileStatic
@Controller
@Secured(SecurityRule.IS_ANONYMOUS)
@Slf4j
class ProfileController implements AdministeredItemReader, ProfileApi {

    static final String MULTI_FACET_AWARE_ITEMS = 'multiFacetAwareItems'
    static final String MULTI_FACET_AWARE_ITEM_DOMAIN_TYPE = 'multiFacetAwareItemDomainType'
    static final String MULTI_FACET_AWARE_ITEM_ID = 'multiFacetAwareItemId'
    static final String PROFILES_PROVIDED = 'ProfilesProvided'
    static final String PROFILE_PROVIDER_SERVICES = 'profileProviderServices'
    static final String NAMESPACE = 'namespace'
    static final String NAME = 'name'
    static final String VERSION = 'version'

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
        AdministeredItem administeredItem = readAdministeredItem(domainType, domainId)
        accessControlService.canDoRole(Role.EDITOR, administeredItem)
        Profile profile = getProfileByName(namespace, name, version)
        handleProfileNotFound(profile, namespace, name, version)
        // Overwrite applied profile with metadata items from the bodyMap
        AppliedProfile appliedProfile = new AppliedProfile(profile, administeredItem, bodyMap)
        List<Metadata> profileMetadata = appliedProfile.metadata
        metadataCacheableRepository.saveAll(profileMetadata)
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

    @Audit
    @Post(Paths.PROFILE_GET_MANY)
    ProfilesProvidedDTO getMany(@NonNull String domainType, @NonNull UUID domainId, @Body Map bodyMap) {
        AdministeredItem model = getAndValidateModel(domainType, domainId)
        List<ProfileProvided> profilesProvided = getProfileProvidedList(model, bodyMap)
        ProfilesProvidedDTO.from(profilesProvided)
    }


    @Audit
    @Post(Paths.PROFILE_VALIDATE_MANY)
    ProfilesProvidedDTO validateMany(@NonNull String domainType, @NonNull UUID domainId, @Body ProfilesProvidedDTO profilesProvidedDTO) {
    //todo
        null
    }

    protected AdministeredItem getAndValidateModel(String domainType, UUID domainId) {
        AdministeredItem model = readAdministeredItem(domainType, domainId)
        pathRepository.readParentItems(model)
        model.updatePath()

        if (!model instanceof Model) {
            ErrorHandler.handleError(HttpStatus.BAD_REQUEST, "This item is not of type model $domainType")
        }
        accessControlService.canDoRole(Role.EDITOR, model)
        return model
    }

    @JsonProperty(PROFILES_PROVIDED)
    protected List<ProfileProvided> getProfileProvidedList(AdministeredItem model, Map bodyMap) {
        List<ProfileProvided> profileProvidedList = []
        List<Profile> matchedProfiles = getMatchedProfiles(bodyMap)
        bodyMap[MULTI_FACET_AWARE_ITEMS].each {
            String domainType = it[MULTI_FACET_AWARE_ITEM_DOMAIN_TYPE]
            UUID itemId = UUID.fromString(it[MULTI_FACET_AWARE_ITEM_ID] as String)
            AdministeredItem administeredItem = findAdministeredItem(domainType, itemId)
            ErrorHandler.handleErrorOnNullObject(HttpStatus.BAD_REQUEST, administeredItem, "item not found $itemId, $domainType")

            if (isValid(administeredItem, model)) {
                matchedProfiles.each {matchedProfile ->
                    AppliedProfile appliedProfile = new AppliedProfile(matchedProfile, administeredItem)
                    profileProvidedList.add(new ProfileProvided(appliedProfile))
                }
            }
        }
        profileProvidedList

    }

    protected boolean isValid(AdministeredItem administeredItem, AdministeredItem modelOwner) {
        pathRepository.readParentItems(administeredItem)
        administeredItem.updatePath()
        if (!administeredItem.path.pathString.contains(modelOwner.path.pathString)){
            ErrorHandler.handleError(HttpStatus.BAD_REQUEST, "Item $administeredItem.id does not belong to Model ${modelOwner.id}")
        }
        true
    }

    protected List<Profile> getMatchedProfiles(Map payloadParamsMap) {
        List<Profile> matchedProfiles = []
        payloadParamsMap[PROFILE_PROVIDER_SERVICES].each {provider ->
            Profile matchedProfile = getProfileByName(provider[NAMESPACE] as String, provider[NAME] as String,
                                                      provider[VERSION] as String)
            if (matchedProfile) {
                matchedProfiles.add(matchedProfile)
            }
        }
        matchedProfiles
    }

    static void handleProfileNotFound(Profile profile, String namespace, String name, String version) {
        if (!profile) {
            throw new HttpStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Profile with namespace: ${namespace}, name: ${name} and version: ${version} not found")
        }
    }

}
