package uk.ac.ox.softeng.mauro.domain.security

import com.fasterxml.jackson.annotation.JsonIgnore
import groovy.transform.AutoClone
import groovy.transform.CompileStatic
import io.micronaut.data.annotation.Index
import io.micronaut.data.annotation.Indexes
import io.micronaut.data.annotation.MappedEntity
import uk.ac.ox.softeng.mauro.domain.model.Item

import java.beans.Transient
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

    byte[] salt
    byte[] password
    String tempPassword

    @JsonIgnore
    @Transient
    String getFullName() {
        "$firstName $lastName"
    }

}
