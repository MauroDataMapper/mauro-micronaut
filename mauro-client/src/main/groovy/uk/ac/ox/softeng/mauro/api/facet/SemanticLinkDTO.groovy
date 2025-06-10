package uk.ac.ox.softeng.mauro.api.facet

import uk.ac.ox.softeng.mauro.domain.facet.SemanticLinkType
import uk.ac.ox.softeng.mauro.domain.model.AdministeredItem

class SemanticLinkDTO {
    UUID id
    String linkType
    final String domainType="SemanticLink"

    UUID targetMultiFacetAwareItemId
    String targetMultiFacetAwareItemDomainType
    Boolean unconfirmed

    AdministeredItem sourceMultiFacetAwareItem
    AdministeredItem targetMultiFacetAwareItem
}
