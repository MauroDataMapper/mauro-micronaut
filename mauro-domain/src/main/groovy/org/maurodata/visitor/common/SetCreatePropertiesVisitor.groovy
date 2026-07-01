package org.maurodata.visitor.common

import org.maurodata.domain.model.Item
import org.maurodata.domain.security.CatalogueUser
import org.maurodata.visitor.GenericDomainTraversalVisitor

class SetCreatePropertiesVisitor extends GenericDomainTraversalVisitor {


    CatalogueUser catalogueUser

    SetCreatePropertiesVisitor(CatalogueUser catalogueUser = null) {
        this.catalogueUser = catalogueUser

        onEnter(Item) {Item item ->
            item.version = null
            item.dateCreated = null
            item.lastUpdated = null
            item.catalogueUser = this.catalogueUser
        }

   }
}
