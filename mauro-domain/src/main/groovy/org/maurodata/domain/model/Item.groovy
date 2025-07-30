package org.maurodata.domain.model

import org.maurodata.domain.security.CatalogueUser

import com.fasterxml.jackson.annotation.JsonAlias
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import groovy.transform.AutoClone
import groovy.transform.CompileStatic
import io.micronaut.core.annotation.Nullable
import io.micronaut.data.annotation.DateCreated
import io.micronaut.data.annotation.DateUpdated
import io.micronaut.data.annotation.GeneratedValue
import io.micronaut.data.annotation.Id
import io.micronaut.data.annotation.MappedProperty
import io.micronaut.data.annotation.Version
import jakarta.persistence.Transient

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
    @JsonAlias(['date_created'])
    Instant dateCreated

    /**
     * The date and time that this object was last updated, as an instant in UTC.
     */
    @DateUpdated
    @JsonAlias(['last_updated'])
    Instant lastUpdated

    /**
     * The email address / username of the user who created this object.
     */
    @Nullable
    @JsonAlias(['created_by'])
    @MappedProperty('created_by')
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @JsonDeserialize(converter = CatalogueUser.StringCatalogueUserConverter)
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

    /**
     * The unchanging immutable identity of an object. This value persists between versions.
     * Set when the object is created for the first time
     */
    @Nullable
    UUID stableId
}
