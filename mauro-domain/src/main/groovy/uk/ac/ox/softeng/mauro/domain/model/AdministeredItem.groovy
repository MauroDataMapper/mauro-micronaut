package uk.ac.ox.softeng.mauro.domain.model

import com.fasterxml.jackson.annotation.JsonIgnore
import groovy.transform.CompileStatic
import groovy.transform.MapConstructor
import io.micronaut.core.annotation.Introspected
import io.micronaut.core.annotation.Nullable
import io.micronaut.data.annotation.DateCreated
import io.micronaut.data.annotation.DateUpdated
import io.micronaut.data.annotation.GeneratedValue
import io.micronaut.data.annotation.Id
import io.micronaut.data.annotation.Index
import io.micronaut.data.annotation.Indexes
import io.micronaut.data.annotation.MappedEntity
import io.micronaut.data.annotation.Version
import io.micronaut.validation.Validated
import jakarta.persistence.Transient
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern

import java.time.OffsetDateTime

@CompileStatic
abstract class AdministeredItem {

    @Id
    @GeneratedValue
    UUID id

    @Version
    Integer version

    @DateCreated
    OffsetDateTime dateCreated

    @DateUpdated
    OffsetDateTime lastUpdated

    @NotBlank
    @Pattern(regexp = /[^\$@|]*/, message = 'Cannot contain $, | or @')
    String label

    @Nullable
    String description

    @Nullable
    String aliasesString

    @Transient
    String domainType = this.class.simpleName

    @Nullable
    String createdBy

    @Nullable
    String path // should be Path type

    @Nullable
    UUID breadcrumbTreeId // should be BreadcrumbTree type

    @Transient
    @JsonIgnore
    abstract AdministeredItem getParent()

    @Transient
    @JsonIgnore
    Model getOwner() {
        parent.owner
    }

    UUID id(UUID id) {
        this.id = id
    }

    UUID id(String id) {
        this.id = UUID.fromString(id)
    }


    String label(String label) {
        this.label = label
    }

    OffsetDateTime dateCreated(OffsetDateTime dateCreated) {
        this.dateCreated = dateCreated
    }

    OffsetDateTime dateCreated(String dateCreated) {
        this.dateCreated = OffsetDateTime.parse(dateCreated)
    }

    OffsetDateTime lastUpdated(OffsetDateTime lastUpdated) {
        this.lastUpdated = lastUpdated
    }

    OffsetDateTime lastUpdated(String lastUpdated) {
        this.lastUpdated = OffsetDateTime.parse(lastUpdated)
    }

    String description(String description) {
        this.description = description
    }

    String aliasesString(String aliasesString) {
        this.aliasesString = aliasesString
    }

    String createdBy(String createdBy) {
        this.createdBy = createdBy
    }

}
