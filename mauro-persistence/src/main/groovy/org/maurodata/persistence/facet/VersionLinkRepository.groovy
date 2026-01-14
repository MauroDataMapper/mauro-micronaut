package org.maurodata.persistence.facet

import groovy.transform.CompileStatic
import io.micronaut.core.annotation.NonNull
import io.micronaut.core.annotation.Nullable
import io.micronaut.data.annotation.Query
import io.micronaut.data.jdbc.annotation.JdbcRepository
import io.micronaut.data.model.query.builder.sql.Dialect

import org.maurodata.domain.facet.VersionLink

@CompileStatic
@JdbcRepository(dialect = Dialect.POSTGRES)
abstract class VersionLinkRepository implements FacetRepository<VersionLink>{

    @Query(''' select target_model_id from core.version_link v where v.multi_facet_aware_item_id = :id ''')
    @Nullable
    abstract UUID findTargetModel(@NonNull UUID id)

    @Query(''' select * from core.version_link v where v.target_model_id  = :id ''')
    @Nullable
    abstract Set<VersionLink> findSourceModels(@NonNull UUID id)

    @Override
    Class getDomainClass() {
        VersionLink
    }
    Boolean handlesPathPrefix(final String pathPrefix) {
        'vl'.equalsIgnoreCase(pathPrefix)
    }
}

