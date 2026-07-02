package org.maurodata.visitor.common

import org.maurodata.domain.model.Item
import org.maurodata.domain.security.CatalogueUser
import org.maurodata.visitor.GenericDomainTraversalVisitor

class RemoveIdVisitor extends GenericDomainTraversalVisitor {

    RemoveIdVisitor() {

        onEnter(Item) {Item item ->
            item.id = null
        }

   }
}
