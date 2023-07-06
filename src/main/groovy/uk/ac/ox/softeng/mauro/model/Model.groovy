package uk.ac.ox.softeng.mauro.model

import com.fasterxml.jackson.annotation.JsonProperty
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

import java.time.OffsetDateTime

@CompileStatic
@MappedEntity
@Indexes(@Index(columns = ['label'], unique = true))
@Introspected
abstract class Model {

    @Id
    @GeneratedValue
    UUID id

    @Version
    Integer version

    @DateCreated
    OffsetDateTime dateCreated

    @DateUpdated
    @JsonProperty
    OffsetDateTime lastUpdated

    @Nullable
    String createdBy

    @Nullable
    String path // should be Path type

    @Nullable
    String aliasesString

    @Nullable
    UUID breadcrumbTreeId // should be BreadcrumbTree type

    Boolean finalised = false

    @Nullable
    OffsetDateTime dateFinalised

    @Nullable
    String documentationVersion

    Boolean readableByEveryone = false

    Boolean readableByAuthenticatedUsers = false

    String modelType

    @Nullable
    String organisation

    Boolean deleted = false

    @Nullable
    String author

    @Nullable
    UUID folderId // -> Folder

    @Nullable
    UUID authorityId // -> Authority

    @Nullable
    String branchName

    @Nullable
    String modelVersion

    @Nullable
    String modelVersionTag

    String label

    @Nullable
    String description
}
