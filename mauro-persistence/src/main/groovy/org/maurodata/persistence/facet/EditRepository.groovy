package org.maurodata.persistence.facet

import org.maurodata.domain.facet.Edit
import org.maurodata.persistence.model.ItemRepository
import org.maurodata.web.ListResponse
import org.maurodata.web.PaginationParams

import groovy.transform.CompileStatic
import io.micronaut.core.annotation.Nullable
import io.micronaut.data.annotation.Query
import io.micronaut.data.jdbc.annotation.JdbcRepository
import io.micronaut.data.model.query.builder.sql.Dialect

@CompileStatic
@JdbcRepository(dialect = Dialect.POSTGRES)
abstract class EditRepository implements FacetRepository<Edit> {

    ListResponse<Edit> readListResponseByMultiFacetAwareItemId(UUID ownerId, @Nullable PaginationParams params) {
        if (params == null) {
            return ListResponse.from(readAllByMultiFacetAwareItemId(ownerId) as List<Edit>, params)
        }

        Long total = countAllByMultiFacetAwareItemId(ownerId)
        List<Edit> edits
        String sort = params.sort ?: 'dateCreated'

        if ((params.max != null && params.max <= 0) || params.all?.equalsIgnoreCase(Boolean.TRUE.toString())) {
            edits = params.order?.equalsIgnoreCase('desc')
                ? readAllByMultiFacetAwareItemIdAndSortDesc(ownerId, sort)
                : readAllByMultiFacetAwareItemIdAndSortAsc(ownerId, sort)
        } else {
            Integer max = params.max ?: 50
            Integer offset = Math.max(0, params.offset ?: 0)
            edits = params.order?.equalsIgnoreCase('desc')
                ? readAllByMultiFacetAwareItemIdAndSortDesc(ownerId, sort, max, offset)
                : readAllByMultiFacetAwareItemIdAndSortAsc(ownerId, sort, max, offset)
        }

        ListResponse.from(edits, total)
    }

    abstract Long countAllByMultiFacetAwareItemId(UUID ownerId)

    @Query('''SELECT *
              FROM core.edit
              WHERE multi_facet_aware_item_id = :ownerId
              ORDER BY
                CASE WHEN :sort = 'dateCreated' THEN date_created END ASC NULLS FIRST,
                CASE WHEN :sort = 'lastUpdated' THEN last_updated END ASC NULLS FIRST,
                CASE WHEN :sort = 'description' THEN description END ASC NULLS FIRST,
                CASE WHEN :sort = 'title' THEN title END ASC NULLS FIRST''')
    abstract List<Edit> readAllByMultiFacetAwareItemIdAndSortAsc(UUID ownerId, String sort)

    @Query('''SELECT *
              FROM core.edit
              WHERE multi_facet_aware_item_id = :ownerId
              ORDER BY
                CASE WHEN :sort = 'dateCreated' THEN date_created END DESC NULLS LAST,
                CASE WHEN :sort = 'lastUpdated' THEN last_updated END DESC NULLS LAST,
                CASE WHEN :sort = 'description' THEN description END DESC NULLS LAST,
                CASE WHEN :sort = 'title' THEN title END DESC NULLS LAST''')
    abstract List<Edit> readAllByMultiFacetAwareItemIdAndSortDesc(UUID ownerId, String sort)

    @Query('''SELECT *
              FROM core.edit
              WHERE multi_facet_aware_item_id = :ownerId
              ORDER BY
                CASE WHEN :sort = 'dateCreated' THEN date_created END ASC NULLS FIRST,
                CASE WHEN :sort = 'lastUpdated' THEN last_updated END ASC NULLS FIRST,
                CASE WHEN :sort = 'description' THEN description END ASC NULLS FIRST,
                CASE WHEN :sort = 'title' THEN title END ASC NULLS FIRST
              LIMIT :max OFFSET :offset''')
    abstract List<Edit> readAllByMultiFacetAwareItemIdAndSortAsc(UUID ownerId, String sort, Integer max, Integer offset)

    @Query('''SELECT *
              FROM core.edit
              WHERE multi_facet_aware_item_id = :ownerId
              ORDER BY
                CASE WHEN :sort = 'dateCreated' THEN date_created END DESC NULLS LAST,
                CASE WHEN :sort = 'lastUpdated' THEN last_updated END DESC NULLS LAST,
                CASE WHEN :sort = 'description' THEN description END DESC NULLS LAST,
                CASE WHEN :sort = 'title' THEN title END DESC NULLS LAST
              LIMIT :max OFFSET :offset''')
    abstract List<Edit> readAllByMultiFacetAwareItemIdAndSortDesc(UUID ownerId, String sort, Integer max, Integer offset)

    @Override
    Class getDomainClass() {
        Edit
    }

    Boolean handlesPathPrefix(final String pathPrefix) {
        'ed'.equalsIgnoreCase(pathPrefix)
    }

}
