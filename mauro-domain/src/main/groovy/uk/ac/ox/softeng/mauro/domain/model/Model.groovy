package uk.ac.ox.softeng.mauro.domain.model

import uk.ac.ox.softeng.mauro.domain.folder.Folder

import com.fasterxml.jackson.annotation.JsonIgnore
import groovy.transform.CompileStatic
import io.micronaut.core.annotation.Nullable
import uk.ac.ox.softeng.mauro.domain.model.version.ModelVersion

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
