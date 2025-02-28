package uk.ac.ox.softeng.mauro.persistence.facet

import groovy.transform.CompileStatic
import io.micronaut.core.annotation.NonNull
import io.micronaut.core.annotation.Nullable
import io.micronaut.data.annotation.Query
import io.micronaut.data.jdbc.annotation.JdbcRepository
import io.micronaut.data.model.query.builder.sql.Dialect
import uk.ac.ox.softeng.mauro.domain.facet.Metadata
import uk.ac.ox.softeng.mauro.domain.facet.VersionLink
import uk.ac.ox.softeng.mauro.persistence.model.ItemRepository

@CompileStatic
@JdbcRepository(dialect = Dialect.POSTGRES)
abstract class VersionLinkRepository implements ItemRepository<VersionLink>{

    @Query(''' select multi_facet_aware_item_id from core.version_link v where v.target_model_id = :id ''')
    @Nullable
    abstract UUID findSourceModel(@NonNull UUID id)

    @Override
    Class getDomainClass() {
        VersionLink
    }
}

