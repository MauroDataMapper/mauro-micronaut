package uk.ac.ox.softeng.mauro.persistence.facet

import groovy.transform.CompileStatic
import io.micronaut.data.jdbc.annotation.JdbcRepository
import io.micronaut.data.model.query.builder.sql.Dialect
import uk.ac.ox.softeng.mauro.domain.facet.Metadata
import uk.ac.ox.softeng.mauro.persistence.model.ItemRepository

@CompileStatic
@JdbcRepository(dialect = Dialect.POSTGRES)
abstract class MetadataRepository implements ItemRepository<Metadata> {

    @Override
    Class getDomainClass() {
        Metadata
    }
}
