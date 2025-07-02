package org.maurodata.persistence.facet

import groovy.transform.CompileStatic
import io.micronaut.data.jdbc.annotation.JdbcRepository
import io.micronaut.data.model.query.builder.sql.Dialect
import org.maurodata.domain.facet.SummaryMetadata
import org.maurodata.persistence.model.ItemRepository

@CompileStatic
@JdbcRepository(dialect = Dialect.POSTGRES)
abstract class SummaryMetadataRepository implements ItemRepository<SummaryMetadata> {

    @Override
    Class getDomainClass() {
        SummaryMetadata
    }
    Boolean handlesPathPrefix(final String pathPrefix) {
        'sm'.equalsIgnoreCase(pathPrefix)
    }
}
