package org.maurodata.persistence.facet

import org.maurodata.domain.facet.SemanticLink
import org.maurodata.persistence.model.ItemRepository

import groovy.transform.CompileStatic
import io.micronaut.data.jdbc.annotation.JdbcRepository
import io.micronaut.data.model.query.builder.sql.Dialect

@CompileStatic
@JdbcRepository(dialect = Dialect.POSTGRES)
abstract class SemanticLinkRepository implements FacetRepository<SemanticLink> {


    abstract Set<SemanticLink> readAllByTargetMultiFacetAwareItemId(UUID ownerId)

    abstract Set<SemanticLink> readAllByTargetMultiFacetAwareItemIdIn(Collection<UUID> ownerIds)


    @Override
    Class getDomainClass() {
        SemanticLink
    }
    Boolean handlesPathPrefix(final String pathPrefix) {
        'sl'.equalsIgnoreCase(pathPrefix)
    }
}
