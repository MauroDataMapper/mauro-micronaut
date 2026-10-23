package org.maurodata.service.link

import groovy.transform.CompileStatic
import org.maurodata.domain.model.AdministeredItem

@CompileStatic
interface ItemLinkGraphProvider {
    String getType()
    String getLabel()
    List<ItemLinkGraphReference> findLinksOwnedByTarget(AdministeredItem target)
    List<ItemLinkGraphReference> findTargetsReferencingSource(AdministeredItem source)
}
