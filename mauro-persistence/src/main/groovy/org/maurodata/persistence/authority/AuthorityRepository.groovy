package org.maurodata.persistence.authority

import org.maurodata.domain.authority.Authority
import org.maurodata.persistence.model.ItemRepository

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

    @Query('select * from core.authority where label = :label')
    abstract Authority findByLabel(String label)

    Class getDomainType() {
        Authority
    }

    @Override
    Class getDomainClass() {
        Authority
    }

}

