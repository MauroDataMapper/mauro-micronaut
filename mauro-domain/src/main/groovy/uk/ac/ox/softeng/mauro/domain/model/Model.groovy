package uk.ac.ox.softeng.mauro.domain.model

import groovy.transform.MapConstructor
import groovy.transform.NamedParams
import uk.ac.ox.softeng.mauro.domain.folder.Folder

import com.fasterxml.jackson.annotation.JsonIgnore
import groovy.transform.CompileStatic
import io.micronaut.core.annotation.Nullable
import uk.ac.ox.softeng.mauro.domain.model.version.ModelVersion

import jakarta.persistence.Transient
import uk.ac.ox.softeng.mauro.domain.terminology.Term

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


    /****
     * Methods for building a tree-like DSL
     */

    Boolean finalised(Boolean finalised) {
        this.finalised = finalised
    }


    OffsetDateTime dateFinalised(OffsetDateTime dateFinalised) {
        this.dateFinalised = dateFinalised
    }

    String documentationVersion(String documentationVersion) {
        this.documentationVersion = documentationVersion
    }

    Boolean readableByEveryone(Boolean readableByEveryone) {
        this.readableByEveryone = readableByEveryone
    }

    Boolean readableByAuthenticatedUsers(Boolean readableByAuthenticatedUsers) {
        this.readableByAuthenticatedUsers = readableByAuthenticatedUsers
    }

    Boolean deleted(Boolean deleted) {
        this.deleted = deleted
    }

    String modelType(String modelType) {
        this.modelType = modelType
    }

    String organisation(String organisation) {
        this.organisation = organisation
    }

    String author(String author) {
        this.author = author
    }

    String branchName(String branchName) {
        this.branchName = branchName
    }

    ModelVersion modelVersion(ModelVersion modelVersion) {
        this.modelVersion = modelVersion
    }

    ModelVersion modelVersion(@NamedParams Map args = [:], @DelegatesTo(value = ModelVersion.class, strategy = Closure.DELEGATE_FIRST) Closure closure) {
        ModelVersion modelVersion = ModelVersion.build(args, closure)
        this.modelVersion = modelVersion
    }

    ModelVersion modelVersion(String versionStr) {
        ModelVersion modelVersion = ModelVersion.from(versionStr)
        this.modelVersion = modelVersion
    }

    String modelVersionTag(String modelVersionTag) {
        this.modelVersionTag = modelVersionTag
    }

}
