package org.maurodata.service.link

import groovy.transform.CompileStatic
import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.maurodata.domain.datamodel.DataClass
import org.maurodata.domain.datamodel.DataModel
import org.maurodata.domain.model.AdministeredItem
import org.maurodata.domain.model.Path
import org.maurodata.persistence.cache.FacetCacheableRepository
import org.maurodata.persistence.facet.dto.ItemLinkGraphRow

@CompileStatic
@Singleton
class DataFlowItemLinkGraphProvider implements ItemLinkGraphProvider {
    private static final int TARGET_ID_BATCH_SIZE = 500

    @Inject FacetCacheableRepository.ItemLinkCacheableRepository itemLinkRepository

    @Override
    String getType() { 'DataFlow' }

    @Override
    String getLabel() { 'Data flow' }

    @Override
    List<ItemLinkGraphReference> findLinksOwnedByTargets(Collection<UUID> targetIds) {
        if (!targetIds) return []
        List<ItemLinkGraphReference> references = []
        (targetIds as List<UUID>).unique().collate(TARGET_ID_BATCH_SIZE).each { List<UUID> batch ->
            references.addAll(toReferences(itemLinkRepository.readGraphRowsByMultiFacetAwareItemIdInAndType(batch, type)))
        }
        references
    }

    @Override
    List<ItemLinkGraphReference> findLinksOwnedByDirectChildren(AdministeredItem parent) {
        if (parent instanceof DataModel) {
            return toReferences(itemLinkRepository.readGraphRowsOwnedByDataModelChildren(parent.id, type))
        }
        if (parent instanceof DataClass) {
            return toReferences(itemLinkRepository.readGraphRowsOwnedByDataClassChildren(parent.id, type))
        }
        []
    }

    private static List<ItemLinkGraphReference> toReferences(List<ItemLinkGraphRow> rows) {
        (rows ?: []).collect { ItemLinkGraphRow row ->
            new ItemLinkGraphReference(
                id: row.id,
                targetId: row.targetId,
                type: row.type,
                sourcePath: row.path ? new Path(row.path) : null
            )
        }
    }
}
