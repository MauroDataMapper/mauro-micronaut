package uk.ac.ox.softeng.mauro.domain.folder

import uk.ac.ox.softeng.mauro.domain.model.AdministeredItem

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import groovy.transform.CompileStatic
import io.micronaut.data.annotation.Indexes
import io.micronaut.core.annotation.Introspected
import io.micronaut.core.annotation.Nullable
import io.micronaut.data.annotation.Index
import io.micronaut.data.annotation.MappedEntity
import io.micronaut.data.annotation.MappedProperty
import io.micronaut.data.annotation.Relation
import jakarta.persistence.Transient
import uk.ac.ox.softeng.mauro.domain.model.Model

import jakarta.validation.constraints.Null

/**
 * A folder is a container for models, and, in the case of a VersionedFolder, may be a model in its own right.
 * <p>
 * Each model must be stored in a folder, and folders may contain other folders (subfolders).
 * Folders must be uniquely named within their parent folder (or the root, if it is a top-level folder), and may
 * be known by other names.
 * A folder may also specify user access control rules for its contents - the user groups with permission to read
 * or write models within.  Folder privileges propagate to the folders below.
 */

@CompileStatic
@Introspected
@MappedEntity(schema = 'core')
@Indexes([@Index(columns = ['parent_folder_id'])])
class Folder extends Model {

    @JsonIgnore
    @Nullable
    Folder parentFolder

    @Transient
    String aliasesString

    @Transient
    String modelType = domainType

    @SuppressWarnings('PropertyName')
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
    @Nullable
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
    List<List<AdministeredItem>> getAllAssociations() {
        [childFolders] as List<List<AdministeredItem>>
    }
}