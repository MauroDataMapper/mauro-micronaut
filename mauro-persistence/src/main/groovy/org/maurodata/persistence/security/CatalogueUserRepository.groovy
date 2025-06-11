package org.maurodata.persistence.security

import groovy.transform.CompileStatic
import io.micronaut.core.annotation.Nullable
import io.micronaut.data.jdbc.annotation.JdbcRepository
import io.micronaut.data.model.query.builder.sql.Dialect
import org.maurodata.domain.security.CatalogueUser
import org.maurodata.persistence.model.ItemRepository

@CompileStatic
@JdbcRepository(dialect = Dialect.POSTGRES)
abstract class CatalogueUserRepository implements ItemRepository<CatalogueUser> {

    @Nullable
    abstract CatalogueUser readByEmailAddress(String emailAddress)

    @Override
    Class getDomainClass() {
        CatalogueUser
    }
}
