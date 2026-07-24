package org.maurodata.api.profile.dto

import org.maurodata.api.model.ModelRefDTO
import org.maurodata.plugin.MauroPluginDTO

import groovy.transform.CompileStatic
import io.micronaut.core.annotation.Introspected

@CompileStatic
@Introspected
class ProfileProvidedDTO implements Serializable {

    AppliedProfilePayloadDTO profile
    ModelRefDTO multiFacetAwareItem
    MauroPluginDTO profileProviderService
    List<String> errors = []
}
