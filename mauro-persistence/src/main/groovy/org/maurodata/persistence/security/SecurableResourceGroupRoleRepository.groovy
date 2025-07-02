package org.maurodata.persistence.security

import groovy.transform.CompileStatic
import io.micronaut.data.jdbc.annotation.JdbcRepository
import io.micronaut.data.model.query.builder.sql.Dialect
import org.maurodata.domain.security.Role
import org.maurodata.domain.security.SecurableResourceGroupRole
import org.maurodata.persistence.model.ItemRepository

@CompileStatic
@JdbcRepository(dialect = Dialect.POSTGRES)
abstract class SecurableResourceGroupRoleRepository implements ItemRepository<SecurableResourceGroupRole> {

    abstract List<SecurableResourceGroupRole> readAllBySecurableResourceDomainTypeAndSecurableResourceId(String securableResourceDomainType, UUID securableResourceId)

    abstract Long deleteBySecurableResourceDomainTypeAndSecurableResourceIdAndRoleAndUserGroupId(String securableResourceDomainType, UUID securableResourceId, Role role, UUID userGroupId)

    @Override
    Class getDomainClass() {
        SecurableResourceGroupRole
    }

    // Not pathable
    Boolean handlesPathPrefix(final String pathPrefix) {
        false
    }
}
