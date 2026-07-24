package org.maurodata.api.profile.dto

import groovy.transform.CompileStatic
import io.micronaut.core.annotation.Introspected

@CompileStatic
@Introspected
class MultiFacetAwareItemRefDTO implements Serializable {

    String multiFacetAwareItemDomainType
    UUID multiFacetAwareItemId
}
