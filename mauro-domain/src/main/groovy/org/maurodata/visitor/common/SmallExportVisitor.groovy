package org.maurodata.visitor.common

import org.maurodata.domain.model.AdministeredItem
import org.maurodata.domain.model.Item
import org.maurodata.domain.model.Model
import org.maurodata.visitor.GenericDomainTraversalVisitor

class SmallExportVisitor extends GenericDomainTraversalVisitor {

    SmallExportVisitor() {
        onEnter(Item) {Item item ->
            item.dateCreated = null
            item.lastUpdated = null
            item.version = null
            item.catalogueUser = null
        }
        onEnter(AdministeredItem) {AdministeredItem administeredItem ->
            administeredItem.edits = []
        }
        onEnter(Model) {Model model ->
            model.readableByAuthenticatedUsers = null
            model.readableByEveryone = null
            model.finalised = null
            model.modelType = null
            model.deleted = null
        }
    }
}
