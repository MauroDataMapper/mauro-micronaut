package org.maurodata.persistence.security

import org.maurodata.domain.security.ApiKey
import org.maurodata.persistence.model.ItemRepository

import groovy.transform.CompileStatic
import io.micronaut.data.jdbc.annotation.JdbcRepository
import io.micronaut.data.model.query.builder.sql.Dialect

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
