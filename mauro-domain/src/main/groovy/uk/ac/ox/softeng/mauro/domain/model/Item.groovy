package uk.ac.ox.softeng.mauro.domain.model

import com.fasterxml.jackson.annotation.JsonAlias
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import groovy.transform.AutoClone
import groovy.transform.CompileStatic
import io.micronaut.core.annotation.Nullable
import io.micronaut.data.annotation.*
import jakarta.persistence.Transient
import uk.ac.ox.softeng.mauro.domain.security.CatalogueUser

import java.time.Instant

/**
 * Item is a base class for domain objects that can be stored in the database.
 */
@CompileStatic
@AutoClone
abstract class Item implements Serializable {

    /**
     * The identity of an object.  UUIDs should be universally unique.
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

    /**
     * The date and time that this object was created, as an instant in UTC.
     */
    @DateCreated
    @JsonDeserialize(converter = InstantConverter)
    @JsonAlias(['date_created'])
    Instant dateCreated

    /**
     * The date and time that this object was last updated, as an instant in UTC.
     */
    @DateUpdated
    @JsonDeserialize(converter = InstantConverter)
    @JsonAlias(['last_updated'])
    Instant lastUpdated

    /**
     * The email address / username of the user who created this object.
     */
    @Nullable
    @JsonAlias(['created_by'])
    @MappedProperty('created_by')
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    CatalogueUser catalogueUser

    @Transient
    String getCreatedBy() {
        catalogueUser?.emailAddress
    }

    /**
     * The domainType of an object is the (simple name of the) concrete class that it instantiates.
     */
    @Transient
    String domainType = this.class.simpleName

    void updateCreationProperties() {
        id = null
        version = null
        dateCreated = null
        lastUpdated = null
    }
}
