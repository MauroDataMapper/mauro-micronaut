package org.maurodata.api.facet


import org.maurodata.domain.model.AdministeredItem

class SemanticLinkCreateDTO {
    UUID id
    String linkType
    UUID targetMultiFacetAwareItemId
    String targetMultiFacetAwareItemDomainType
}
