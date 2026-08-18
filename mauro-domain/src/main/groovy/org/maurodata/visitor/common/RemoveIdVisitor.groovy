package org.maurodata.visitor.common

import org.maurodata.domain.facet.Facet
import org.maurodata.domain.model.Item
import org.maurodata.domain.security.CatalogueUser
import org.maurodata.visitor.GenericDomainTraversalVisitor

class RemoveIdVisitor extends GenericDomainTraversalVisitor {

    RemoveIdVisitor() {

        onEnter(Item) {Item item ->
            item.id = null
        }

        onEnter(Facet) {Facet facet ->
            facet.multiFacetAwareItemId = null
            facet.multiFacetAwareItemDomainType = null
        }
   }
}
