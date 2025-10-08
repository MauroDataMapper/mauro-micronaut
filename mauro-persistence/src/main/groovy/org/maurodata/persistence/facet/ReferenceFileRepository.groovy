package org.maurodata.persistence.facet

import groovy.transform.CompileStatic
import io.micronaut.core.annotation.Nullable
import io.micronaut.data.jdbc.annotation.JdbcRepository
import io.micronaut.data.model.query.builder.sql.Dialect
import org.maurodata.domain.facet.ReferenceFile
import org.maurodata.persistence.model.ItemRepository

@CompileStatic
@JdbcRepository(dialect = Dialect.POSTGRES)
abstract class ReferenceFileRepository implements FacetRepository<ReferenceFile> {

    @Nullable
    abstract long deleteById(UUID referenceFileId)

    @Override
    Class getDomainClass() {
        ReferenceFile
    }
    Boolean handlesPathPrefix(final String pathPrefix) {
        'rf'.equalsIgnoreCase(pathPrefix)
    }
}
