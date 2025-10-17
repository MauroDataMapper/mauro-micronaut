package org.maurodata.domain.security

import org.maurodata.domain.model.ItemUtils

import groovy.transform.AutoClone
import groovy.transform.CompileStatic
import io.micronaut.core.annotation.Nullable
import io.micronaut.data.annotation.Index
import io.micronaut.data.annotation.Indexes
import io.micronaut.data.annotation.MappedEntity
import io.micronaut.data.annotation.Relation
import org.maurodata.domain.model.Item

import jakarta.persistence.Transient

@CompileStatic
@MappedEntity(schema = 'security')
@AutoClone
@Indexes([@Index(columns = ['name'], unique = true)])
class UserGroup extends Item {

    String name

    @Nullable
    String description

    Boolean undeletable

    ApplicationRole applicationRole

    @Relation(Relation.Kind.MANY_TO_MANY)
    Set<CatalogueUser> groupMembers = []

    @Override
    void copyInto(Item into) {
        super.copyInto(into)
        UserGroup intoUserGroup = (UserGroup) into
        intoUserGroup.name = ItemUtils.copyItem(this.name, intoUserGroup.name)
        intoUserGroup.description = ItemUtils.copyItem(this.description, intoUserGroup.description)
        intoUserGroup.undeletable = ItemUtils.copyItem(this.undeletable, intoUserGroup.undeletable)
        intoUserGroup.applicationRole = ItemUtils.copyItem(this.applicationRole, intoUserGroup.applicationRole)
        intoUserGroup.groupMembers = ItemUtils.copyItems(this.groupMembers, intoUserGroup.groupMembers)
    }

    @Override
    Item shallowCopy() {
        UserGroup userGroupShallowCopy = new UserGroup()
        this.copyInto(userGroupShallowCopy)
        return userGroupShallowCopy
    }

    @Transient
    List<String> availableActions = []
}
