package org.maurodata.profile

import com.fasterxml.jackson.annotation.JsonAlias
import groovy.transform.CompileStatic
import io.micronaut.core.annotation.Introspected

@Introspected
@CompileStatic
class ProfilesProvidedDTO<ProfileProvided>{
    Integer count

    @JsonAlias('profilesProvided')
    List<ProfileProvided> profilesProvided

    static ProfilesProvidedDTO from(List profilesProvided) {
        new ProfilesProvidedDTO(count: profilesProvided.size(), profilesProvided: profilesProvided ?: [])
    }
}
