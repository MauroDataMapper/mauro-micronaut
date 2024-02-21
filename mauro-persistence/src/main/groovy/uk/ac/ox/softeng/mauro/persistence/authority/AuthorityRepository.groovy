package uk.ac.ox.softeng.mauro.persistence.authority

import groovy.transform.CompileStatic
import io.micronaut.data.jdbc.annotation.JdbcRepository
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.repository.PageableRepository
import uk.ac.ox.softeng.mauro.domain.authority.Authority

@CompileStatic
@JdbcRepository(dialect = Dialect.POSTGRES)
abstract class AuthorityRepository implements PageableRepository<Authority, UUID> {

    Class getDomainType() {
        Authority
    }
}
