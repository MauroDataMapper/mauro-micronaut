package uk.ac.ox.softeng.mauro.persistence.facet

import uk.ac.ox.softeng.mauro.domain.facet.Rule
import uk.ac.ox.softeng.mauro.persistence.model.ItemRepository

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
