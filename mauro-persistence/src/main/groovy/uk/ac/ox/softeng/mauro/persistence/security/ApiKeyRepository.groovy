package uk.ac.ox.softeng.mauro.persistence.security

import uk.ac.ox.softeng.mauro.domain.email.Email
import uk.ac.ox.softeng.mauro.domain.security.ApiKey
import uk.ac.ox.softeng.mauro.domain.security.CatalogueUser
import uk.ac.ox.softeng.mauro.persistence.model.ItemRepository

import groovy.transform.CompileStatic
import io.micronaut.core.annotation.NonNull
import io.micronaut.core.annotation.Nullable
import io.micronaut.data.jdbc.annotation.JdbcRepository
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.repository.PageableRepository
import jakarta.validation.Valid

@CompileStatic
@JdbcRepository(dialect = Dialect.POSTGRES)
abstract class ApiKeyRepository implements ItemRepository<ApiKey> {

    abstract List<ApiKey> readByCatalogueUserId(UUID catalogueUserId)

    @Override
    Class getDomainClass() {
        ApiKey
    }

}
