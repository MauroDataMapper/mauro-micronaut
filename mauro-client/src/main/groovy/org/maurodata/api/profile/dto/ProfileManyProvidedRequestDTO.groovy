package org.maurodata.api.profile.dto

import groovy.transform.CompileStatic
import io.micronaut.core.annotation.Introspected

@CompileStatic
@Introspected
class ProfileManyProvidedRequestDTO implements Serializable {

    Integer count
    List<ProfileProvidedRequestDTO> profilesProvided = []
}
