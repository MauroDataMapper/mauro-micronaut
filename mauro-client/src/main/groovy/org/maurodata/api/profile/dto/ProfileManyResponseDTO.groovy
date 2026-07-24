package org.maurodata.api.profile.dto

import groovy.transform.CompileStatic
import io.micronaut.core.annotation.Introspected

@CompileStatic
@Introspected
class ProfileManyResponseDTO implements Serializable {

    Integer count
    List<ProfileProvidedDTO> profilesProvided = []
}
