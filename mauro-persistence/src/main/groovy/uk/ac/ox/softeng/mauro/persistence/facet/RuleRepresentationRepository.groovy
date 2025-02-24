package uk.ac.ox.softeng.mauro.persistence.facet

import uk.ac.ox.softeng.mauro.domain.facet.RuleRepresentation
import uk.ac.ox.softeng.mauro.persistence.model.ItemRepository

import groovy.transform.CompileStatic
import io.micronaut.core.annotation.NonNull
import io.micronaut.core.annotation.Nullable
import io.micronaut.data.annotation.Query
import io.micronaut.data.jdbc.annotation.JdbcRepository
import io.micronaut.data.model.query.builder.sql.Dialect

@CompileStatic
@JdbcRepository(dialect = Dialect.POSTGRES)
abstract class RuleRepresentationRepository implements ItemRepository<RuleRepresentation> {

    @Query(''' select * from core.rule_representation rr where rr.rule_id = :ruleId ''')
    @Nullable
    abstract List<RuleRepresentation> findAllByRuleId(@NonNull UUID ruleId)


    @Override
    Class getDomainClass() {
        RuleRepresentation
    }
}
