package org.maurodata.service.link

import groovy.transform.CompileStatic
import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.maurodata.domain.facet.ItemLink
import org.maurodata.domain.model.AdministeredItem
import org.maurodata.domain.model.Path
import org.maurodata.persistence.cache.FacetCacheableRepository
import org.maurodata.persistence.model.PathRepository

@CompileStatic
@Singleton
class DataFlowItemLinkGraphProvider implements ItemLinkGraphProvider {
    @Inject FacetCacheableRepository.ItemLinkCacheableRepository itemLinkRepository
    @Inject PathRepository pathRepository

    @Override
    String getType() { 'DataFlow' }

    @Override
    String getLabel() { 'Data flow' }

    @Override
    List<ItemLinkGraphReference> findLinksOwnedByTarget(AdministeredItem target) {
        (itemLinkRepository.readAllByMultiFacetAwareItemIdAndType(target.id, type) as List<ItemLink>)
            .collect { ItemLink itemLink -> reference(itemLink) }
            .sort(false) { ItemLinkGraphReference reference -> reference.sourcePath?.toString() ?: '' }
    }

    @Override
    List<ItemLinkGraphReference> findTargetsReferencingSource(AdministeredItem source) {
        pathRepository.readParentItems(source)
        source.updatePath()
        Path absoluteSourcePath = source.path
        if (!absoluteSourcePath) return []

        List<String> candidatePaths = [absoluteSourcePath.toString(), absoluteSourcePath.localPath().toString()].unique()
            .findAll { String path -> path }
        if (!candidatePaths) return []

        (itemLinkRepository.readAllByTypeAndPathIn(type, candidatePaths) as List<ItemLink>)
            .collect { ItemLink itemLink -> reference(itemLink) }
            .sort(false) { ItemLinkGraphReference reference ->
                "${reference.targetDomainType?.toLowerCase() ?: ''}:${reference.targetId}:${reference.sourcePath?.toString() ?: ''}"
            }
    }

    private ItemLinkGraphReference reference(ItemLink itemLink) {
        new ItemLinkGraphReference(
            id: itemLink.id,
            type: itemLink.type,
            sourcePath: itemLink.path,
            targetId: itemLink.multiFacetAwareItemId,
            targetDomainType: itemLink.multiFacetAwareItemDomainType
        )
    }
}
