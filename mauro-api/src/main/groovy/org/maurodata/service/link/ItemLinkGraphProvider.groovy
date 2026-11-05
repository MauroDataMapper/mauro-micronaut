package org.maurodata.service.link

import groovy.transform.CompileStatic
import org.maurodata.domain.model.AdministeredItem
@CompileStatic
interface ItemLinkGraphProvider {
    String getType()
    String getLabel()
    List<ItemLinkGraphReference> findLinksOwnedByTargets(Collection<UUID> targetIds)
    List<ItemLinkGraphReference> findLinksOwnedByDirectChildren(AdministeredItem parent)
}
