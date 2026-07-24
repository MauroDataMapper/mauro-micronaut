package org.maurodata.api.profile.dto

import org.maurodata.plugin.MauroPluginDTO

import groovy.transform.CompileStatic
import io.micronaut.core.annotation.Introspected

@CompileStatic
@Introspected
class ProfileManyGetRequestDTO implements Serializable {

    List<MultiFacetAwareItemRefDTO> multiFacetAwareItems = []
    List<MauroPluginDTO> profileProviderServices = []
}
