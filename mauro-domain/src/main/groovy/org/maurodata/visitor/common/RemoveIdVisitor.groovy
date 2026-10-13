package org.maurodata.visitor.common

import groovy.transform.CompileStatic
import org.maurodata.domain.facet.Facet
import org.maurodata.domain.model.Item
import org.maurodata.visitor.GenericDomainTraversalVisitor
import org.maurodata.visitor.ItemHandler

@CompileStatic
class RemoveIdVisitor extends GenericDomainTraversalVisitor implements ItemHandler {

    RemoveIdVisitor() {
        addHandler(this)
    }

    @Override
    void enter(Item item) {
        item.id = null
        if (item instanceof Facet) {
            Facet facet = (Facet) item
            facet.multiFacetAwareItemId = null
            facet.multiFacetAwareItemDomainType = null
        }
    }

    @Override
    void leave(Item item) {
    }
}
