package org.maurodata.profile

import groovy.transform.CompileStatic
import io.micronaut.core.annotation.Introspected

@Introspected
@CompileStatic
class ProfileProviderService {

    String namespace
    String name
    String version

    ProfileProviderService(String namespace, String name, String version) {
        this.namespace = namespace
        this.name = name
        this.version = version
    }
}
