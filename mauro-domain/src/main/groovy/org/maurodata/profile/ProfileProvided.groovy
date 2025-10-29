package org.maurodata.profile


import com.fasterxml.jackson.annotation.JsonProperty
import groovy.transform.CompileStatic
import io.micronaut.core.annotation.Introspected
import org.maurodata.profile.applied.AppliedProfile

@Introspected
@CompileStatic
class ProfileProvided {
    @JsonProperty('profile')
    AppliedProfile appliedProfile
    ProfileProviderService profileProviderService

    ProfileProvided(AppliedProfile appliedProfile) {
        this.appliedProfile = appliedProfile
        this.profileProviderService =  new ProfileProviderService(appliedProfile.getNamespace(), appliedProfile.getName(), appliedProfile.getVersion())
    }

}
