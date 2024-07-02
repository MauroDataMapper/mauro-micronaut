package uk.ac.ox.softeng.mauro.persistence.config

import groovy.transform.CompileStatic
import io.micronaut.core.annotation.Nullable
import io.micronaut.data.annotation.Join
import io.micronaut.data.jdbc.annotation.JdbcRepository
import io.micronaut.data.model.query.builder.sql.Dialect
import uk.ac.ox.softeng.mauro.domain.config.ApiProperty
import uk.ac.ox.softeng.mauro.persistence.model.ItemRepository

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
}
