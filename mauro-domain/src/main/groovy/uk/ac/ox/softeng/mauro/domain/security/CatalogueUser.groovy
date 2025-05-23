package uk.ac.ox.softeng.mauro.domain.security

import uk.ac.ox.softeng.mauro.domain.model.Item

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
    String profilePicture // should be UserImageFile type
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

    CatalogueUser() {}

    CatalogueUser(String identity) {
        this.id = UUID.fromString(identity)
    }

    CatalogueUser(UUID identity) {
        this.id = identity
    }


}
