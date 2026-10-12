package org.maurodata.visitor.common

import groovy.transform.CompileStatic
import org.maurodata.domain.model.AdministeredItem
import org.maurodata.domain.model.Item
import org.maurodata.domain.model.Model
import org.maurodata.visitor.GenericDomainTraversalVisitor
import org.maurodata.visitor.ItemHandler

@CompileStatic
class SmallExportVisitor extends GenericDomainTraversalVisitor implements ItemHandler {

    SmallExportVisitor() {
        addHandler(this)
    }

    @Override
    void enter(Item item) {
        item.dateCreated = null
        item.lastUpdated = null
        item.version = null
        item.catalogueUser = null
        if (item instanceof AdministeredItem) {
            ((AdministeredItem) item).edits = []
        }
        if (item instanceof Model) {
            Model model = (Model) item
            model.readableByAuthenticatedUsers = null
            model.readableByEveryone = null
            model.finalised = null
            model.modelType = null
            model.deleted = null
        }
    }

    @Override
    void leave(Item item) {
    }
}
