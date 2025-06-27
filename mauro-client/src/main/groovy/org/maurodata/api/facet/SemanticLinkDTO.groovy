package org.maurodata.api.facet

import groovy.transform.CompileStatic
import io.micronaut.core.annotation.Introspected
import org.maurodata.api.model.ModelRefDTO
import org.maurodata.domain.model.AdministeredItem

@Introspected
@CompileStatic
class SemanticLinkDTO {
    UUID id
    String linkType
    final String domainType = "SemanticLink"
    Boolean unconfirmed

    ModelRefDTO sourceMultiFacetAwareItem
    ModelRefDTO targetMultiFacetAwareItem
}
