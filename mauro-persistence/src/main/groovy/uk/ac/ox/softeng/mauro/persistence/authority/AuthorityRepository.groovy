package uk.ac.ox.softeng.mauro.persistence.authority

import uk.ac.ox.softeng.mauro.domain.authority.Authority
import uk.ac.ox.softeng.mauro.persistence.model.ItemRepository

import groovy.transform.CompileStatic
import io.micronaut.core.annotation.Nullable
import io.micronaut.data.annotation.Query
import io.micronaut.data.jdbc.annotation.JdbcRepository
import io.micronaut.data.model.query.builder.sql.Dialect

@CompileStatic
@JdbcRepository(dialect = Dialect.POSTGRES)
abstract class AuthorityRepository implements ItemRepository<Authority> {

    @Nullable
    @Query('select * from core.authority where default_authority = :defaultAuthority')
    abstract Authority findByDefaultAuthority(boolean defaultAuthority)

    @Nullable
    @Query('select * from core.authority')
    abstract List<Authority> findAll()

    Class getDomainType() {
        Authority
    }

    @Override
    Class getDomainClass() {
        Authority
    }
}

