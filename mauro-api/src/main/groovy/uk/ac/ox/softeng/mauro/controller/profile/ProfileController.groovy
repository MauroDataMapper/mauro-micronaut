package uk.ac.ox.softeng.mauro.controller.profile

import uk.ac.ox.softeng.mauro.controller.model.AdministeredItemReader
import uk.ac.ox.softeng.mauro.domain.model.AdministeredItem

import groovy.transform.CompileStatic
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import jakarta.inject.Inject
import uk.ac.ox.softeng.mauro.persistence.profile.DynamicProfileService
import uk.ac.ox.softeng.mauro.profile.DataModelBasedProfile
import uk.ac.ox.softeng.mauro.profile.Profile
import uk.ac.ox.softeng.mauro.profile.ProfileService

@CompileStatic
@Controller
class ProfileController implements AdministeredItemReader {

    @Inject
    ProfileService profileService

    @Inject
    DynamicProfileService dynamicProfileService

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
        AdministeredItem administeredItem = readAdministeredItem(domainType, domainId)
        profileService.getUsedProfilesForAdministeredItem(getAllProfiles(), administeredItem)
    }

    @Get('/{domainType}/{domainId}/profiles/unused')
    List<Profile> getUnusedProfiles(String domainType, UUID domainId) {
        AdministeredItem administeredItem = readAdministeredItem(domainType, domainId)
        profileService.getUnusedProfilesForAdministeredItem(getAllProfiles(), administeredItem)
    }



}
