package org.maurodata.domain.security

import org.maurodata.domain.model.ItemUtils

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

    @Override
    void copyInto(Item into) {
        super.copyInto(into)
        SecurableResourceGroupRole intoSecurableResourceGroupRole = (SecurableResourceGroupRole) into
        intoSecurableResourceGroupRole.securableResourceDomainType =
            ItemUtils.copyItem(this.securableResourceDomainType, intoSecurableResourceGroupRole.securableResourceDomainType)
        intoSecurableResourceGroupRole.securableResourceId = ItemUtils.copyItem(this.securableResourceId, intoSecurableResourceGroupRole.securableResourceId)
        intoSecurableResourceGroupRole.userGroup = ItemUtils.copyItem(this.userGroup, intoSecurableResourceGroupRole.userGroup)
        intoSecurableResourceGroupRole.role = ItemUtils.copyItem(this.role, intoSecurableResourceGroupRole.role)
    }

    @Override
    Item shallowCopy() {
        SecurableResourceGroupRole securableResourceGroupRoleShallowCopy = new SecurableResourceGroupRole()
        this.copyInto(securableResourceGroupRoleShallowCopy)
        return securableResourceGroupRoleShallowCopy
    }
}
