package org.maurodata.api.facet

import groovy.transform.CompileStatic

@CompileStatic
class SemanticLinkCreateDTO {
    UUID id
    String linkType
    UUID targetMultiFacetAwareItemId
    String targetMultiFacetAwareItemDomainType
}
