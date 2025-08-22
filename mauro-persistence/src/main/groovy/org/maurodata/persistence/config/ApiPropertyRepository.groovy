package org.maurodata.persistence.config

import groovy.transform.CompileStatic
import io.micronaut.core.annotation.Nullable
import io.micronaut.data.annotation.Join
import io.micronaut.data.jdbc.annotation.JdbcRepository
import io.micronaut.data.model.query.builder.sql.Dialect
import org.maurodata.domain.config.ApiProperty
import org.maurodata.persistence.model.ItemRepository

@CompileStatic
@JdbcRepository(dialect = Dialect.POSTGRES)
abstract class ApiPropertyRepository implements ItemRepository<ApiProperty> {

    @Override
    Class getDomainClass() {
        ApiProperty
    }

    @Nullable
    @Join(value = 'catalogueUser', type = Join.Type.LEFT_FETCH)
    @Join(value = 'lastUpdatedBy', type = Join.Type.LEFT_FETCH)
    abstract ApiProperty findById(UUID id)

    @Nullable
    @Join(value = 'catalogueUser', type = Join.Type.LEFT_FETCH)
    @Join(value = 'lastUpdatedBy', type = Join.Type.LEFT_FETCH)
    abstract List<ApiProperty> findByPubliclyVisibleTrue()

    @Nullable
    @Join(value = 'catalogueUser', type = Join.Type.LEFT_FETCH)
    @Join(value = 'lastUpdatedBy', type = Join.Type.LEFT_FETCH)
    abstract List<ApiProperty> findAll()

    // Does not have a prefix
    Boolean handlesPathPrefix(final String pathPrefix) {
        false
    }
}
