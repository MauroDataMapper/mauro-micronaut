package org.maurodata.domain.model

import groovy.transform.CompileStatic
import io.micronaut.core.annotation.Introspected

@Introspected
@CompileStatic
class Breadcrumb {

    UUID id
    String domainType
    String label
    Boolean finalised
}
