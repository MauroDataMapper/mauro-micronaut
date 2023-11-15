package uk.ac.ox.softeng.mauro.domain.model

import uk.ac.ox.softeng.mauro.domain.authority.Authority
import uk.ac.ox.softeng.mauro.domain.security.SecurableResource

import com.fasterxml.jackson.annotation.JsonProperty
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
abstract class Model extends AdministeredItem implements SecurableResource {

    Boolean finalised = false

    @Nullable
    OffsetDateTime dateFinalised

    @Nullable
    String documentationVersion

    Boolean readableByEveryone = false

    Boolean readableByAuthenticatedUsers = false

    String modelType = domainType

    @Nullable
    String organisation

    Boolean deleted = false

    @Nullable
    String author

    @JsonIgnore
    Folder folder

    @Nullable
    Authority authority

    @Nullable
    String branchName = 'main'

    @Nullable
    ModelVersion modelVersion

    @Nullable
    String modelVersionTag

    @Override
    @Transient
    @JsonIgnore
    Model getParent() {
        null
    }

    @Override
    @Transient
    @JsonIgnore
    void setParent(AdministeredItem parent) {
        folder = (Folder) parent
    }

    @Override
    @Transient
    @JsonIgnore
    Model getOwner() {
        this
    }

    @Override
    @Transient
    @JsonIgnore
    String getPathModelIdentifier() {
        modelVersion ?: branchName
    }

    /**
     * For a model which has all its associations loaded, return a collection of all its direct child items.
     * The collection should be ordered so that all the items can be saved in order.
     */
    @Transient
    @JsonIgnore
    abstract Collection<AdministeredItem> getAllContents()

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
