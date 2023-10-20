package uk.ac.ox.softeng.mauro.domain.folder

import uk.ac.ox.softeng.mauro.domain.model.AdministeredItem

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import groovy.transform.CompileStatic
import io.micronaut.core.annotation.Introspected
import io.micronaut.core.annotation.Nullable
import io.micronaut.data.annotation.MappedEntity
import io.micronaut.data.annotation.MappedProperty
import io.micronaut.data.annotation.Relation
import jakarta.persistence.Transient
import uk.ac.ox.softeng.mauro.domain.model.Model

@CompileStatic
@Introspected
@MappedEntity
class Folder extends Model {

    @JsonIgnore
    @Nullable
    Folder parentFolder

    @Transient
    String aliasesString

    @Transient
    String modelType = domainType

    @MappedProperty('class')
    @JsonProperty('class')
    @Nullable
    String class_

    @Transient
    UUID breadcrumbTreeId

    @Transient
    String organisation

    @Transient
    String author

    @Nullable
    String branchName = null

    @JsonIgnore
    @Transient
    @Override
    Folder getFolder() {
        parentFolder
    }

    @JsonIgnore
    @Transient
    @Override
    void setFolder(Folder folder) {
        parentFolder = folder
    }

    @Relation(value = Relation.Kind.ONE_TO_MANY, mappedBy = 'parentFolder')
    List<Folder> childFolders = []

    @Override
    @Transient
    @JsonIgnore
    Folder getParent() {
        parentFolder
    }

    @Override
    @Transient
    @JsonIgnore
    void setParent(AdministeredItem folder) {
        parentFolder = (Folder) folder
    }

    @Override
    @Transient
    @JsonIgnore
    Folder getOwner() {
        parentFolder ? parentFolder.owner : this
    }

    @Override
    @Transient
    @JsonIgnore
    String getPathPrefix() {
        'fo'
    }

    @Override
    @Transient
    @JsonIgnore
    String getPathModelIdentifier() {
        null
    }

    @Override
    @Transient
    @JsonIgnore
    Collection<AdministeredItem> getAllContents() {
        childFolders?.each {it.parentFolder = this}
        childFolders as Collection<AdministeredItem>
    }
}
