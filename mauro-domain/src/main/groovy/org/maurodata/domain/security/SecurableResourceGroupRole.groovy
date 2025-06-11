package org.maurodata.domain.security

import groovy.transform.AutoClone
import groovy.transform.CompileStatic
import io.micronaut.data.annotation.Index
import io.micronaut.data.annotation.Indexes
import io.micronaut.data.annotation.MappedEntity
import org.maurodata.domain.model.Item

@CompileStatic
@MappedEntity(schema = 'security')
@AutoClone
@Indexes([@Index(columns = ['securable_resource_id', 'role'], unique = true)])
class SecurableResourceGroupRole extends Item {

    String securableResourceDomainType
    UUID securableResourceId

    UserGroup userGroup
    Role role
}
