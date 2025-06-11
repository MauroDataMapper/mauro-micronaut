package org.maurodata.persistence.facet

import org.maurodata.domain.facet.Rule
import org.maurodata.persistence.model.ItemRepository

import groovy.transform.CompileStatic
import io.micronaut.data.jdbc.annotation.JdbcRepository
import io.micronaut.data.model.query.builder.sql.Dialect

@CompileStatic
@JdbcRepository(dialect = Dialect.POSTGRES)
abstract class RuleRepository implements ItemRepository<Rule> {

    @Override
    Class getDomainClass() {
        Rule
    }
}
