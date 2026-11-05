package org.maurodata.persistence.facet

import groovy.transform.CompileStatic
import io.micronaut.data.annotation.Query
import io.micronaut.data.jdbc.annotation.JdbcRepository
import io.micronaut.data.model.query.builder.sql.Dialect
import org.maurodata.domain.facet.ItemLink
import org.maurodata.persistence.facet.dto.ItemLinkGraphRow

@CompileStatic
@JdbcRepository(dialect = Dialect.POSTGRES)
abstract class ItemLinkRepository implements FacetRepository<ItemLink> {
    abstract Set<ItemLink> readAllByMultiFacetAwareItemIdAndType(UUID ownerId, String type)
    abstract Set<ItemLink> readAllByMultiFacetAwareItemIdInAndType(Collection<UUID> ownerIds, String type)

    @Query('''select id, multi_facet_aware_item_id as target_id, type, path
              from core.item_link
              where type = :type and multi_facet_aware_item_id in (:ownerIds)
              order by multi_facet_aware_item_id, path''')
    abstract List<ItemLinkGraphRow> readGraphRowsByMultiFacetAwareItemIdInAndType(Collection<UUID> ownerIds, String type)

    @Query('''select link.id, link.multi_facet_aware_item_id as target_id, link.type, link.path
              from core.item_link link
              join (
                select id from datamodel.data_class where data_model_id = :dataModelId and parent_data_class_id is null
                union all
                select id from datamodel.data_component where data_model_id = :dataModelId
              ) child on child.id = link.multi_facet_aware_item_id
              where link.type = :type
              order by link.multi_facet_aware_item_id, link.path''')
    abstract List<ItemLinkGraphRow> readGraphRowsOwnedByDataModelChildren(UUID dataModelId, String type)

    @Query('''select link.id, link.multi_facet_aware_item_id as target_id, link.type, link.path
              from core.item_link link
              join (
                select id from datamodel.data_class where parent_data_class_id = :dataClassId
                union all
                select id from datamodel.data_element where data_class_id = :dataClassId
              ) child on child.id = link.multi_facet_aware_item_id
              where link.type = :type
              order by link.multi_facet_aware_item_id, link.path''')
    abstract List<ItemLinkGraphRow> readGraphRowsOwnedByDataClassChildren(UUID dataClassId, String type)

    @Query('select * from core.item_link where type = :type and path in (:paths)')
    abstract Set<ItemLink> readAllByTypeAndPathIn(String type, Collection<String> paths)

    @Override
    Class<ItemLink> getDomainClass() { ItemLink }

    Boolean handlesPathPrefix(String pathPrefix) { 'il'.equalsIgnoreCase(pathPrefix) }
}
