package uk.ac.ox.softeng.mauro.model

import uk.ac.ox.softeng.mauro.folder.Folder
import uk.ac.ox.softeng.mauro.model.version.ModelVersion

import com.fasterxml.jackson.annotation.JsonIgnore
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
import io.micronaut.data.annotation.TypeDef
import io.micronaut.data.annotation.Version
import io.micronaut.data.model.DataType
import jakarta.persistence.Column

import java.beans.Transient
import java.time.OffsetDateTime

@CompileStatic
abstract class Model extends AdministeredItem {

    Boolean finalised = false

    @Nullable
    OffsetDateTime dateFinalised

    @Nullable
    String documentationVersion

    Boolean readableByEveryone

    Boolean readableByAuthenticatedUsers

    String modelType = domainType

    @Nullable
    String organisation

    Boolean deleted

    @Nullable
    String author

    @Nullable
    @JsonIgnore
    Folder folder

    @Nullable
    UUID authorityId // -> Authority

    @Nullable
    String branchName

    @Nullable
    ModelVersion modelVersion

    @Nullable
    String modelVersionTag

    @Override
    @Transient
    @JsonIgnore
    Folder getParent() {
        folder
    }
}
