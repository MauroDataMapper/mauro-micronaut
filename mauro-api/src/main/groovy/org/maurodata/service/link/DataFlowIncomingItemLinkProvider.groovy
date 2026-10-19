package org.maurodata.service.link

import groovy.transform.CompileStatic
import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.maurodata.domain.facet.ItemLink
import org.maurodata.domain.model.AdministeredItem
import org.maurodata.persistence.cache.FacetCacheableRepository

@CompileStatic
@Singleton
class DataFlowIncomingItemLinkProvider implements IncomingItemLinkProvider {
    @Inject FacetCacheableRepository.ItemLinkCacheableRepository itemLinkRepository

    @Override
    String getType() { 'DataFlow' }

    @Override
    String getLabel() { 'Data flow' }

    @Override
    List<IncomingItemLink> findIncomingLinks(AdministeredItem target) {
        (itemLinkRepository.readAllByMultiFacetAwareItemIdAndType(target.id, type) as List<ItemLink>)
            .collect { ItemLink itemLink ->
                new IncomingItemLink(id: itemLink.id, type: type, path: itemLink.path)
            }
            .sort(false) { IncomingItemLink link -> link.path?.toString() ?: '' }
    }
}
