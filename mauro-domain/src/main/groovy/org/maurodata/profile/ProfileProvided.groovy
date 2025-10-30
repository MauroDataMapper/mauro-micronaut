package org.maurodata.profile

import com.fasterxml.jackson.annotation.JsonAlias
import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import groovy.transform.CompileStatic
import io.micronaut.core.annotation.Introspected
import org.maurodata.profile.applied.AppliedProfile

@Introspected
@CompileStatic
class ProfileProvided {
    @JsonProperty('profile')
    AppliedProfile profile

    ProfileProviderService profileProviderService

    @JsonCreator
    ProfileProvided() {
    }

    ProfileProvided(@JsonAlias('profile') AppliedProfile appliedProfile) {
        this.profile = appliedProfile
        this.profileProviderService =  new ProfileProviderService(appliedProfile.getNamespace(), appliedProfile.getName(), appliedProfile.getVersion())
    }

}
