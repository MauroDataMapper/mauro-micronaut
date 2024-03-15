package uk.ac.ox.softeng.mauro.persistence.facet

import groovy.transform.CompileStatic
import io.micronaut.data.jdbc.annotation.JdbcRepository
import io.micronaut.data.model.query.builder.sql.Dialect
import uk.ac.ox.softeng.mauro.domain.facet.SummaryMetadata
import uk.ac.ox.softeng.mauro.persistence.model.ItemRepository

@CompileStatic
@JdbcRepository(dialect = Dialect.POSTGRES)
abstract class SummaryMetadataRepository implements ItemRepository<SummaryMetadata> {

    @Override
    Class getDomainClass() {
        SummaryMetadata
    }
}
