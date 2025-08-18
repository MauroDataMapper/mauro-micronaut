package org.maurodata.persistence.security

import org.maurodata.domain.email.Email
import org.maurodata.domain.security.ApiKey
import org.maurodata.domain.security.CatalogueUser
import org.maurodata.persistence.model.ItemRepository

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

    abstract List<ApiKey> readByCatalogueUserIdAndName(UUID catalogueUserId, String name)

    @Override
    Class getDomainClass() {
        ApiKey
    }

    // Not currently pathable
    Boolean handlesPathPrefix(final String pathPrefix) {
        false
    }
}
