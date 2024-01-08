package uk.ac.ox.softeng.mauro.domain

import groovy.transform.CompileStatic
import io.micronaut.core.annotation.Introspected
import io.micronaut.core.annotation.Nullable
import io.micronaut.data.annotation.GeneratedValue
import io.micronaut.data.annotation.Id
import io.micronaut.data.annotation.MappedEntity
import io.micronaut.data.annotation.Version
import jakarta.validation.constraints.NotBlank

import java.time.OffsetDateTime

@CompileStatic
@Introspected
@MappedEntity
class Email {

    /**
     * The identify of an object.  UUIDs should be universally unique.
     * Identities are usually created when the object is saved in the database, but can be manually set beforehand.
     */
    @Id
    @GeneratedValue
    UUID id

    /**
     * The version of an object - this is an internal number used for persistence purposes
     */
    @Version
    Integer version


    @NotBlank
    String sentToEmailAddress

    @NotBlank
    String subject

    @NotBlank
    String body

    @NotBlank
    String emailServiceUsed

    @Nullable
    OffsetDateTime dateTimeSent

    @Nullable
    Boolean successfullySent

    @Nullable
    String failureReason


    UUID id(String id) {
        this.id = UUID.fromString(id)
        this.id
    }

    UUID id(UUID id) {
        this.id = id
        this.id
    }

}
