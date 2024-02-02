package uk.ac.ox.softeng.mauro.persistence.security

import groovy.transform.CompileStatic
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.r2dbc.annotation.R2dbcRepository
import reactor.core.publisher.Flux
import uk.ac.ox.softeng.mauro.domain.security.SecurableResourceGroupRole
import uk.ac.ox.softeng.mauro.persistence.model.ItemRepository

@CompileStatic
@R2dbcRepository(dialect = Dialect.POSTGRES)
abstract class SecurableResourceGroupRoleRepository implements ItemRepository<SecurableResourceGroupRole> {

    abstract Flux<SecurableResourceGroupRole> readAllBySecurableResourceDomainTypeAndSecurableResourceId(String securableResourceDomainType, UUID securableResourceId)

    @Override
    Class getDomainClass() {
        SecurableResourceGroupRole
    }
}
