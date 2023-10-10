package uk.ac.ox.softeng.mauro.domain.model

import uk.ac.ox.softeng.mauro.domain.folder.Folder

import com.fasterxml.jackson.annotation.JsonIgnore
import groovy.transform.CompileStatic
import io.micronaut.core.annotation.Nullable
import uk.ac.ox.softeng.mauro.domain.model.version.ModelVersion

import jakarta.persistence.Transient

import java.time.OffsetDateTime

@CompileStatic
abstract class Model extends AdministeredItem {

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

    @Nullable
    @JsonIgnore
    Folder folder

    @Nullable
    UUID authorityId // -> Authority

    @Nullable
    String branchName = 'main'

    @Nullable
    ModelVersion modelVersion

    @Nullable
    String modelVersionTag

    @Override
    @Transient
    @JsonIgnore
    AdministeredItem getParent() {
        null
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
     * For a model which has all its associations loaded, return a collection of all its child items.
     * The collection should be in tree order, so that Paths can be updated in order through the items.
     */
    @Transient
    @JsonIgnore
    abstract Collection<AdministeredItem> getAllContents()
}
