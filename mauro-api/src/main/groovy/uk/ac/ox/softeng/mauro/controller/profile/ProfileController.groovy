package uk.ac.ox.softeng.mauro.controller.profile

import groovy.transform.CompileStatic
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import jakarta.inject.Inject
import uk.ac.ox.softeng.mauro.profile.Profile
import uk.ac.ox.softeng.mauro.profile.ProfileService

@CompileStatic
@Controller('/profiles')
class ProfileController {

    @Inject
    ProfileService profileService

    @Get('/providers')
    List<Profile> providers() {
        profileService.getStaticProfiles()
    }

}
