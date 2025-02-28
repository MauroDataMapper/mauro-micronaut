package uk.ac.ox.softeng.mauro.domain.folder

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import groovy.transform.CompileStatic
import groovy.transform.MapConstructor
import io.micronaut.core.annotation.Introspected
import io.micronaut.core.annotation.Nullable
import io.micronaut.data.annotation.*
import jakarta.persistence.Transient
import uk.ac.ox.softeng.mauro.domain.datamodel.DataModel
import uk.ac.ox.softeng.mauro.domain.model.AdministeredItem
import uk.ac.ox.softeng.mauro.domain.model.Model
import uk.ac.ox.softeng.mauro.domain.terminology.CodeSet
import uk.ac.ox.softeng.mauro.domain.terminology.Terminology

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
@MapConstructor(includeSuperFields = true, includeSuperProperties = true, noArg = true)
class Folder extends Model {

    { versionableFlag=false }

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
    @JsonIgnore
    String class_

    @JsonIgnore
    void setClass_(final String class_)
    {
        this.class_=class_

        if(this.class_!=null && "VersionedFolder" == this.class_)
        {
            setVersionable(true)
        }
        else
        {
            setVersionable(false)
        }
    }

    @Transient
    @Override
    void setVersionable(final boolean versionable)
    {
        this.versionableFlag=versionable
        if(versionable) {
            this.class_ = "VersionedFolder"
        }
        else
        {
            this.class_ = null
        }
    }

    @Transient
    @Override
    String getDomainType()
    {
        if(this.class_!=null && "VersionedFolder" == this.class_)
        {
            return "VersionedFolder"
        }
        else
        {
            return "Folder"
        }
    }


    @Transient
    UUID breadcrumbTreeId

    @Nullable
    String organisation

    @Nullable
    String author

    // TODO: write a test for branch name
    @Nullable
    String branchName = 'main'
    String getBranchName()
    {
        if(this.class_!=null && "VersionedFolder" == this.class_)
        {
            return branchName
        }
        else
        {
            return null
        }
    }

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

    @Relation(value = Relation.Kind.ONE_TO_MANY, mappedBy = 'dataModel')
    List<DataModel> dataModels = []

    @Relation(value = Relation.Kind.ONE_TO_MANY, mappedBy = 'terminology')
    @JsonIgnore
    // have to parse separately due to internal references
    List<Terminology> terminologies = []

    @Relation(value = Relation.Kind.ONE_TO_MANY, mappedBy = 'codeSet')
    List<CodeSet> codeSets = []

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
        this
    }

    @Override
    @Transient
    @JsonIgnore
    String getPathPrefix() {

        if(isVersionable())
        {
            return 'vf'
        }

        return 'fo'
    }

    @Override
    @Transient
    @JsonIgnore
    String getPathModelIdentifier() {
        if(!isVersionable())
        {
            return null
        }
        // I'm a model, you know what I mean
        return super.getPathModelIdentifier()
    }

    @Override
    Folder clone() {
        Folder cloned = (Folder) super.clone()
        cloned.childFolders = childFolders.collect {
            it.clone().tap {
                it.parentFolder = cloned
            }
        }
        cloned.dataModels = dataModels.collect {
            it.clone().tap {
                it.folder = cloned
            }
        }
        cloned.terminologies = terminologies.collect {
            it.clone().tap {
                it.folder = cloned
            }
        }
        cloned.codeSets = codeSets.collect {it.clone()}
        cloned.setAssociations()
        cloned
    }

    @Transient
    @JsonIgnore
    @Override
    void setAssociations() {
        childFolders.each { childFolder ->
            childFolder.parentFolder = this
            childFolder.setAssociations()
        }
        dataModels.each { dataModel ->
            dataModel.folder = this
            dataModel.setAssociations()
        }
        terminologies.each { terminology ->
            terminology.folder = this
            terminology.setAssociations()
        }
        codeSets.each { codeSet ->
            codeSet.folder = this
            codeSet.setAssociations()
        }
    }

    @Override
    @Transient
    @JsonIgnore
    List<List<AdministeredItem>> getAllAssociations() {
        [childFolders] as List<List<AdministeredItem>>
    }

    /****
     * Methods for building a tree-like DSL
     */

    static Folder build(
            Map args,
            @DelegatesTo(value = Folder, strategy = Closure.DELEGATE_FIRST) Closure closure = {}) {
        new Folder(args).tap(closure)
    }

    static Folder build(
            @DelegatesTo(value = Folder, strategy = Closure.DELEGATE_FIRST) Closure closure = {}) {
        build [:], closure
    }
}