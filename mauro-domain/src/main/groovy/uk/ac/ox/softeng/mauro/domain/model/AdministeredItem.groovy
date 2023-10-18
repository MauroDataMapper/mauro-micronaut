package uk.ac.ox.softeng.mauro.domain.model

import com.fasterxml.jackson.annotation.JsonIgnore
import groovy.transform.CompileStatic
import io.micronaut.core.annotation.Nullable
import io.micronaut.data.annotation.DateCreated
import io.micronaut.data.annotation.DateUpdated
import io.micronaut.data.annotation.GeneratedValue
import io.micronaut.data.annotation.Id
import io.micronaut.data.annotation.Version
import jakarta.persistence.Transient
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern

import java.time.OffsetDateTime

/**
 * An AdministeredItem is an item stored in the catalogue.
 * <p>
 * A certain amount of administrative metadata is stored for each item - for example its identifier (UUID), the
 * date/time it was created, and the date/time it was last updated.
 * Every item has, by default, a String-valued label, and a longer description (although some administered items may
 * use other fields for these purposes.
 */

@CompileStatic
abstract class AdministeredItem {

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

    /**
     * The date and time that this object was created
     */

    @DateCreated
    OffsetDateTime dateCreated

    /**
     * The date and time that this object was last updated.
     */

    @DateUpdated
    OffsetDateTime lastUpdated

    /**
     * The label of an object.  Labels are used as identifiers within a context and so need to be unique within
     * that context. Labels of models must be unique when combined with the version number or branch name.
     * <p>
     *     A label cannot contain the characters `$`, `|` or `@`, since this values are used in the creation of paths.
     */

    @NotBlank
    @Pattern(regexp = /[^\$@|]*/, message = 'Cannot contain $, | or @')
    String label

    /**
     * The textual description of an object.  Descriptions can be formatted in plain text, markdown or HTML.
     */
    @Nullable
    String description

    /**
     * A list of other names for this object, separated by semi-colons.  These do not have to be unique.
     */
    @Nullable
    String aliasesString

    /**
     * The domainType of an object is the (simple name of the) concrete class that it instantiates.
     */
    @Transient
    String domainType = this.class.simpleName

    /**
     * The email address / username of the user who created this object.
     */
    @Nullable
    String createdBy

    /**
     * The path of oan object allows it to be navigated to from either the containing model, or the folder path within
     * a system.  This value is calculated on persistence and saved to allow easy lookup.
     */
    @Nullable
    String path // should be Path type

    /**
     * The identifier of a breadcrumb tree object for navigation.
     */
    @Nullable
    UUID breadcrumbTreeId // should be BreadcrumbTree type

    /**
     * Helper method for returning the parent of this object, if one exists.
     */
    @Transient
    @JsonIgnore
    abstract AdministeredItem getParent()

    /**
     * Helper method for returning the owning model of this object, if one exists.
     */
    @Transient
    @JsonIgnore
    Model getOwner() {
        parent.owner
    }

    /**
     * DSL helper method for setting the identifier.  Returns the identifier passed in.
     *
     * @see #id
     */
    UUID id(UUID id) {
        this.id = id
        this.id
    }

    /**
     * DSL helper method for setting the identifier.  Returns the identifier passed in.
     *
     * @see #id
     */
    UUID id(String id) {
        this.id = UUID.fromString(id)
        this.id
    }

    /**
     * DSL helper method for setting the label.  Returns the label passed in.
     *
     * @see #label
     */
    String label(String label) {
        this.label = label
        this.label
    }

    /**
     * DSL helper method for setting the date created.  Returns the date/time passed in.
     *
     * @see #dateCreated
     */
    OffsetDateTime dateCreated(OffsetDateTime dateCreated) {
        this.dateCreated = dateCreated
        this.dateCreated
    }

    /**
     * DSL helper method for setting the date created.  Returns the date/time passed in.
     *
     * @see #dateCreated
     */
    OffsetDateTime dateCreated(String dateCreated) {
        this.dateCreated = OffsetDateTime.parse(dateCreated)
        this.dateCreated
    }

    /**
     * DSL helper method for setting the date this object was last updated.  Returns the date/time passed in.
     *
     * @see #lastUpdated
     */
    OffsetDateTime lastUpdated(OffsetDateTime lastUpdated) {
        this.lastUpdated = lastUpdated
        this.lastUpdated
    }

    /**
     * DSL helper method for setting the date this object was last updated.  Returns the date/time passed in.
     *
     * @see #lastUpdated
     */
    OffsetDateTime lastUpdated(String lastUpdated) {
        this.lastUpdated = OffsetDateTime.parse(lastUpdated)
        this.lastUpdated
    }

    /**
     * DSL helper method for setting the description.  Returns the description passed in.
     *
     * @see #description
     */
    String description(String description) {
        this.description = description
        this.description
    }

    /**
     * DSL helper method for setting the aliases string.  Returns the aliases string passed in.
     *
     * @see #aliasesString
     */
    String aliasesString(String aliasesString) {
        this.aliasesString = aliasesString
        this.aliasesString
    }

    /**
     * DSL helper method for setting the createdBy field.  Returns the createdBy string passed in.
     *
     * @see #createdBy
     */
    String createdBy(String createdBy) {
        this.createdBy = createdBy
        this.createdBy
    }

}
