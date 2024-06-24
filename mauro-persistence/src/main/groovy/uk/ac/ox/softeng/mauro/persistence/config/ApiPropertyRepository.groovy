package uk.ac.ox.softeng.mauro.persistence.config

import groovy.transform.CompileStatic
import io.micronaut.core.annotation.Nullable
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
    abstract List<ApiProperty> readByPubliclyVisibleTrue()

    @Nullable
    abstract List<ApiProperty> readAll()
}
