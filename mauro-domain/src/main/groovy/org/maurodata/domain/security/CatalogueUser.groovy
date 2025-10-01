package org.maurodata.domain.security

import org.maurodata.domain.model.Item
import org.maurodata.domain.model.ItemUtils

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.util.StdConverter
import groovy.transform.AutoClone
import groovy.transform.CompileStatic
import io.micronaut.data.annotation.Index
import io.micronaut.data.annotation.Indexes
import io.micronaut.data.annotation.MappedEntity
import io.micronaut.data.annotation.Relation
import jakarta.persistence.Transient

import java.time.Instant

@CompileStatic
@MappedEntity(schema = 'security')
@AutoClone
@Indexes([@Index(columns = ['email_address'], unique = true)])
class CatalogueUser extends Item {

    String emailAddress

    String firstName
    String lastName
    String jobTitle
    String organisation

    Boolean pending
    Boolean disabled
    String profilePicture
    // should be UserImageFile type
    String userPreferences
    UUID resetToken

    String creationMethod
    Instant lastLogin

    @JsonIgnore
    byte[] salt

    @JsonIgnore
    byte[] password

    @JsonIgnore
    String tempPassword

    @Relation(Relation.Kind.MANY_TO_MANY)
    @JsonDeserialize(converter = CatalogueUserGroupsConverter)
    Set<UserGroup> groups = []

    static class CatalogueUserGroupsConverter extends StdConverter<List<UUID>, Set<UserGroup>> {
        @Override
        Set<UserGroup> convert(List<UUID> groupIds) {
            groupIds.collect {new UserGroup(id: it)} as Set<UserGroup>
        }
    }

    static class StringCatalogueUserConverter extends StdConverter<String, CatalogueUser> {
        @Override
        CatalogueUser convert(String id) {
            new CatalogueUser(id: UUID.fromString(id))
        }
    }

    @JsonIgnore
    @Transient
    String getFullName() {
        "$firstName $lastName"
    }

    CatalogueUser() {
    }

    CatalogueUser(String identity) {
        this.id = UUID.fromString(identity)
    }

    CatalogueUser(UUID identity) {
        this.id = identity
    }

    @Override
    void copyInto(Item into) {
        super.copyInto(into)
        CatalogueUser intoCatalogueUser = (CatalogueUser) into
        intoCatalogueUser.emailAddress = ItemUtils.copyItem(this.emailAddress, intoCatalogueUser.emailAddress)
        intoCatalogueUser.firstName = ItemUtils.copyItem(this.firstName, intoCatalogueUser.firstName)
        intoCatalogueUser.lastName = ItemUtils.copyItem(this.lastName, intoCatalogueUser.lastName)
        intoCatalogueUser.jobTitle = ItemUtils.copyItem(this.jobTitle, intoCatalogueUser.jobTitle)
        intoCatalogueUser.organisation = ItemUtils.copyItem(this.organisation, intoCatalogueUser.organisation)
        intoCatalogueUser.pending = ItemUtils.copyItem(this.pending, intoCatalogueUser.pending)
        intoCatalogueUser.disabled = ItemUtils.copyItem(this.disabled, intoCatalogueUser.disabled)
        intoCatalogueUser.profilePicture = ItemUtils.copyItem(this.profilePicture, intoCatalogueUser.profilePicture)
        intoCatalogueUser.userPreferences = ItemUtils.copyItem(this.userPreferences, intoCatalogueUser.userPreferences)
        intoCatalogueUser.resetToken = ItemUtils.copyItem(this.resetToken, intoCatalogueUser.resetToken)
        intoCatalogueUser.creationMethod = ItemUtils.copyItem(this.creationMethod, intoCatalogueUser.creationMethod)
        intoCatalogueUser.lastLogin = ItemUtils.copyItem(this.lastLogin, intoCatalogueUser.lastLogin)
        intoCatalogueUser.salt = ItemUtils.copyItem(this.salt, intoCatalogueUser.salt)
        intoCatalogueUser.password = ItemUtils.copyItem(this.password, intoCatalogueUser.password)
        intoCatalogueUser.tempPassword = ItemUtils.copyItem(this.tempPassword, intoCatalogueUser.tempPassword)
        intoCatalogueUser.groups = ItemUtils.copyItems(this.groups, intoCatalogueUser.groups)
    }

    @Override
    Item shallowCopy() {
        CatalogueUser catalogueUserShallowCopy = new CatalogueUser()
        this.copyInto(catalogueUserShallowCopy)
        return catalogueUserShallowCopy
    }
}
