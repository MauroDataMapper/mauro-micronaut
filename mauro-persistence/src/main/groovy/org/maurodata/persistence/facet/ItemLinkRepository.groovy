package org.maurodata.persistence.facet

import groovy.transform.CompileStatic
import io.micronaut.data.annotation.Query
import io.micronaut.data.jdbc.annotation.JdbcRepository
import io.micronaut.data.model.query.builder.sql.Dialect
import org.maurodata.domain.facet.ItemLink

@CompileStatic
@JdbcRepository(dialect = Dialect.POSTGRES)
abstract class ItemLinkRepository implements FacetRepository<ItemLink> {
    abstract Set<ItemLink> readAllByMultiFacetAwareItemIdAndType(UUID ownerId, String type)
    @Query('select * from core.item_link where type = :type and path in (:paths)')
    abstract Set<ItemLink> readAllByTypeAndPathIn(String type, Collection<String> paths)

    @Override
    Class<ItemLink> getDomainClass() { ItemLink }

    Boolean handlesPathPrefix(String pathPrefix) { 'il'.equalsIgnoreCase(pathPrefix) }
}
