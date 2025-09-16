package org.maurodata.domain.folder

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import groovy.transform.CompileStatic
import groovy.transform.MapConstructor
import io.micronaut.core.annotation.Introspected
import io.micronaut.core.annotation.Nullable
import io.micronaut.data.annotation.*
import jakarta.persistence.Transient
import org.maurodata.domain.classifier.ClassificationScheme
import org.maurodata.domain.datamodel.DataModel
import org.maurodata.domain.model.AdministeredItem
import org.maurodata.domain.model.Model
import org.maurodata.domain.model.ModelItem
import org.maurodata.domain.terminology.CodeSet
import org.maurodata.domain.terminology.Terminology

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

/*
    @Override
    @Transient
    @JsonIgnore
    List<Collection<? extends ModelItem<Folder>>> getAllAssociations() {
        [childFolders, dataModels, terminologies, codeSets, classificationSchemes] as List<Collection<? extends ModelItem<Folder>>>
    }
*/

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

    @Override
    String getBranchName()
    {
        if(this.class_!=null && "VersionedFolder" == this.class_)
        {
            return super.branchName
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

    @Relation(value = Relation.Kind.ONE_TO_MANY, mappedBy = 'classificationScheme')
    List<ClassificationScheme> classificationSchemes = []

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
    @Nullable
    String getPathModelIdentifier() {
        if(!isVersionable())
        {
            return null
        }
        // I'm a model, you know what I mean
        return super.getPathModelIdentifier()
    }

    @Transient
    @JsonIgnore
    String getDiffIdentifier() {
        if (parentFolder != null) {return "${parentFolder.getDiffIdentifier()}|${getPathNodeString()}"}
        return "${getPathNodeString()}"
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
        cloned.classificationSchemes = classificationSchemes.collect {
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
        classificationSchemes.each { classificationScheme ->
            classificationScheme.folder = this
            classificationScheme.setAssociations()
        }
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

    Folder folder(Folder folder) {
        this.childFolders.add(folder)
        folder.parent = this
        folder
    }

    Folder folder(Map args, @DelegatesTo(value = Folder, strategy = Closure.DELEGATE_FIRST) Closure closure = {}) {
        Folder folder1 = build(args + [parent: this], closure)
        folder folder1
    }

    Folder folder(@DelegatesTo(value = Folder, strategy = Closure.DELEGATE_FIRST) Closure closure = {}) {
        folder [:], closure
    }

}