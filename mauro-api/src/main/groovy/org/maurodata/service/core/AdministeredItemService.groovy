package org.maurodata.service.core

import jakarta.inject.Inject
import org.maurodata.domain.model.AdministeredItem
import org.maurodata.persistence.model.PathRepository

abstract class AdministeredItemService {

    @Inject
    PathRepository pathRepository

    AdministeredItem updateDerivedProperties(AdministeredItem item) {
        pathRepository.readParentItems(item)
        item.updatePath()
        item.updateBreadcrumbs()
        item
    }

    protected void updateCreationProperties(AdministeredItem item) {
        item.id = null
        item.version = null
        item.dateCreated = null
        item.lastUpdated = null
    }

    AdministeredItem updatePaths(AdministeredItem administeredItem) {
        updateDerivedProperties(administeredItem)
        administeredItem.getAllAssociations().each {
            it.each {assoc ->
                assoc.each {
                    updateDerivedProperties(assoc)
                    updatePaths(assoc)
                }
            }
        }
        administeredItem
    }
}
