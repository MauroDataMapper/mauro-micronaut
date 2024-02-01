package uk.ac.ox.softeng.mauro.persistence.security

import groovy.transform.CompileStatic
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.r2dbc.annotation.R2dbcRepository
import reactor.core.publisher.Mono
import uk.ac.ox.softeng.mauro.persistence.model.ItemRepository
import uk.ac.ox.softeng.mauro.domain.security.CatalogueUser

@CompileStatic
@R2dbcRepository(dialect = Dialect.POSTGRES)
abstract class CatalogueUserRepository implements ItemRepository<CatalogueUser> {

    abstract Mono<CatalogueUser> readByEmailAddress(String emailAddress)

    @Override
    Class getDomainClass() {
        CatalogueUser
    }
}
