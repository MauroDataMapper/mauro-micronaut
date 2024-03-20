package uk.ac.ox.softeng.mauro.persistence.security

import groovy.transform.CompileStatic
import io.micronaut.data.jdbc.annotation.JdbcRepository
import io.micronaut.data.model.query.builder.sql.Dialect
import uk.ac.ox.softeng.mauro.domain.security.SecurableResourceGroupRole
import uk.ac.ox.softeng.mauro.persistence.model.ItemRepository

@CompileStatic
@JdbcRepository(dialect = Dialect.POSTGRES)
abstract class SecurableResourceGroupRoleRepository implements ItemRepository<SecurableResourceGroupRole> {

    abstract List<SecurableResourceGroupRole> readAllBySecurableResourceDomainTypeAndSecurableResourceId(String securableResourceDomainType, UUID securableResourceId)

    @Override
    Class getDomainClass() {
        SecurableResourceGroupRole
    }
}
