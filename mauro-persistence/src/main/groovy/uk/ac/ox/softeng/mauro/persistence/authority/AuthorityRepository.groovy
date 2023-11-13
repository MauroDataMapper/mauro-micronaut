package uk.ac.ox.softeng.mauro.persistence.authority

import uk.ac.ox.softeng.mauro.domain.authority.Authority
import uk.ac.ox.softeng.mauro.persistence.model.AdministeredItemRepository

import groovy.transform.CompileStatic
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.r2dbc.annotation.R2dbcRepository
import io.micronaut.data.repository.reactive.ReactorPageableRepository

@CompileStatic
@R2dbcRepository(dialect = Dialect.POSTGRES)
abstract class AuthorityRepository implements ReactorPageableRepository<Authority, UUID>, AdministeredItemRepository<Authority> {

    @Override
    Boolean handles(Class clazz) {
        clazz == Authority
    }

    @Override
    Boolean handles(String domainType) {
        domainType.toLowerCase() in ['authority', 'authorities']
    }
}
