package org.maurodata.service.core

import jakarta.inject.Inject
import org.maurodata.domain.model.AdministeredItem
import org.maurodata.persistence.model.PathRepository

abstract class AdministeredItemService {

    @Inject
    PathRepository pathRepository

    protected AdministeredItem updateDerivedProperties(AdministeredItem item) {
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
}
