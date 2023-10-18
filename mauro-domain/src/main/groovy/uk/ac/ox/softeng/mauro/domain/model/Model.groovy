package uk.ac.ox.softeng.mauro.domain.model

import groovy.transform.NamedParams
import uk.ac.ox.softeng.mauro.domain.folder.Folder

import com.fasterxml.jackson.annotation.JsonIgnore
import groovy.transform.CompileStatic
import io.micronaut.core.annotation.Nullable
import uk.ac.ox.softeng.mauro.domain.model.version.ModelVersion

import jakarta.persistence.Transient

import java.time.OffsetDateTime

/**
 * A model in Mauro is a document containing a description of existing data, or a specification concerning the
 * constraints on data that is to be collected or transferred
 * <p>
 * Models are top-level administered items that may describe a data asset, a data standard, a terminology (or subset
 * of one), or some reference data.  Models may be published and versioned, and may specify controls on who can access
 * or modify them.
 * <p>
 * All models must be stored within a folder.
 */
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
        this.finalised
    }

    OffsetDateTime dateFinalised(OffsetDateTime dateFinalised) {
        this.dateFinalised = dateFinalised
        this.dateFinalised
    }

    String documentationVersion(String documentationVersion) {
        this.documentationVersion = documentationVersion
        this.documentationVersion
    }

    Boolean readableByEveryone(Boolean readableByEveryone) {
        this.readableByEveryone = readableByEveryone
        this.readableByEveryone
    }

    Boolean readableByAuthenticatedUsers(Boolean readableByAuthenticatedUsers) {
        this.readableByAuthenticatedUsers = readableByAuthenticatedUsers
        this.readableByAuthenticatedUsers
    }

    Boolean deleted(Boolean deleted) {
        this.deleted = deleted
        this.deleted
    }

    String modelType(String modelType) {
        this.modelType = modelType
        this.modelType
    }

    String organisation(String organisation) {
        this.organisation = organisation
        this.organisation
    }

    String author(String author) {
        this.author = author
        this.author
    }

    String branchName(String branchName) {
        this.branchName = branchName
        this.branchName
    }

    ModelVersion modelVersion(ModelVersion modelVersion) {
        this.modelVersion = modelVersion
        this.modelVersion
    }

    ModelVersion modelVersion(
            @NamedParams Map args,
            @DelegatesTo(value = ModelVersion, strategy = Closure.DELEGATE_FIRST) Closure closure = { }) {
        this.modelVersion = ModelVersion.build(args, closure)
        this.modelVersion
    }

    ModelVersion modelVersion(
            @DelegatesTo(value = ModelVersion, strategy = Closure.DELEGATE_FIRST) Closure closure = { }) {
        this.modelVersion = ModelVersion.build([:], closure)
        this.modelVersion
    }

    ModelVersion modelVersion(String versionStr) {
        this.modelVersion = ModelVersion.from(versionStr)
        modelVersion
    }

    String modelVersionTag(String modelVersionTag) {
        this.modelVersionTag = modelVersionTag
        this.modelVersionTag
    }

}
