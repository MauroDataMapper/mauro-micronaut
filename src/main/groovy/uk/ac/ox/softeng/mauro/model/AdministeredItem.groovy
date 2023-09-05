package uk.ac.ox.softeng.mauro.model

import groovy.transform.CompileStatic
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
import jakarta.persistence.Transient

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
}
