package uk.ac.ox.softeng.mauro.domain.security

import uk.ac.ox.softeng.mauro.domain.model.Item

import java.time.Instant

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


}
