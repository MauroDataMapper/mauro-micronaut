package org.maurodata.visitor.common

import groovy.transform.CompileStatic
import org.maurodata.domain.model.Item
import org.maurodata.domain.security.CatalogueUser
import org.maurodata.visitor.GenericDomainTraversalVisitor
import org.maurodata.visitor.ItemHandler

@CompileStatic
class SetCreatePropertiesVisitor extends GenericDomainTraversalVisitor implements ItemHandler {

    CatalogueUser catalogueUser

    SetCreatePropertiesVisitor(CatalogueUser catalogueUser = null) {
        this.catalogueUser = catalogueUser
        addHandler(this)
    }

    @Override
    void enter(Item item) {
        item.version = null
        item.dateCreated = null
        item.lastUpdated = null
        item.catalogueUser = this.catalogueUser
    }

    @Override
    void leave(Item item) {
    }
}
