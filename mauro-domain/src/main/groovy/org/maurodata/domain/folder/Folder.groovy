package org.maurodata.domain.folder

import org.maurodata.domain.model.Item
import org.maurodata.domain.model.ItemReference
import org.maurodata.domain.model.ItemReferencer
import org.maurodata.domain.model.ItemReferencerUtils
import org.maurodata.domain.model.ItemUtils

import com.fasterxml.jackson.annotation.JsonIgnore
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
class Folder extends Model implements ItemReferencer {

    @JsonIgnore
    @Nullable
    Folder parentFolder

    @Transient
    String aliasesString

    @Transient
    String modelType = domainType

    @Transient
    @Override
    String getDomainType() {
        if(branchName || modelVersion) {
            return "VersionedFolder"
        } else {
            return "Folder"
        }
    }

    @Transient
    UUID breadcrumbTreeId

    @Nullable
    String organisation

    @Nullable
    String author


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
    Model getOwner() {
        getModelWithVersion() ?: this
    }

    @Override
    @Transient
    @JsonIgnore
    String getPathPrefix() {
        if (domainType == "VersionedFolder") {
            return 'vf'
        }
        return 'fo'
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
        super.setAssociations()
        childFolders.each {childFolder ->
            childFolder.parentFolder = this
            childFolder.setAssociations()
        }
        dataModels.each {dataModel ->
            dataModel.folder = this
            dataModel.setAssociations()
        }
        terminologies.each {terminology ->
            terminology.folder = this
            terminology.setAssociations()
        }
        codeSets.each {codeSet ->
            codeSet.folder = this
            codeSet.setAssociations()
        }
        classificationSchemes.each {classificationScheme ->
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


    @Transient
    @JsonIgnore
    @Override
    List<ItemReference> retrieveItemReferences() {
        List<ItemReference> pathsBeingReferenced = [] + super.retrieveItemReferences()

        ItemReferencerUtils.addItems(childFolders, pathsBeingReferenced)
        ItemReferencerUtils.addItems(dataModels, pathsBeingReferenced)
        ItemReferencerUtils.addItems(terminologies, pathsBeingReferenced)
        ItemReferencerUtils.addItems(codeSets, pathsBeingReferenced)
        ItemReferencerUtils.addItems(classificationSchemes, pathsBeingReferenced)

        return pathsBeingReferenced
    }

    @Transient
    @JsonIgnore
    @Override
    void replaceItemReferencesByIdentity(IdentityHashMap<Item, Item> replacements, Map<UUID, Item> allItemsById, List<Item> notReplaced) {
        super.replaceItemReferencesByIdentity(replacements, allItemsById, notReplaced)
        parentFolder = ItemReferencerUtils.replaceItemByIdentity(parentFolder, replacements, notReplaced)
        childFolders = ItemReferencerUtils.replaceItemsByIdentity(childFolders, replacements, notReplaced)
        dataModels = ItemReferencerUtils.replaceItemsByIdentity(dataModels, replacements, notReplaced)
        terminologies = ItemReferencerUtils.replaceItemsByIdentity(terminologies, replacements, notReplaced)
        codeSets = ItemReferencerUtils.replaceItemsByIdentity(codeSets, replacements, notReplaced)
        classificationSchemes = ItemReferencerUtils.replaceItemsByIdentity(classificationSchemes, replacements, notReplaced)
    }

    @Override
    void copyInto(Item into) {
        super.copyInto(into)
        Folder intoFolder = (Folder) into
        intoFolder.parentFolder = ItemUtils.copyItem(this.parentFolder, intoFolder.parentFolder)
        intoFolder.aliasesString = ItemUtils.copyItem(this.aliasesString, intoFolder.aliasesString)
        intoFolder.modelType = ItemUtils.copyItem(this.modelType, intoFolder.modelType)
        intoFolder.breadcrumbTreeId = ItemUtils.copyItem(this.breadcrumbTreeId, intoFolder.breadcrumbTreeId)
        intoFolder.organisation = ItemUtils.copyItem(this.organisation, intoFolder.organisation)
        intoFolder.author = ItemUtils.copyItem(this.author, intoFolder.author)
        intoFolder.childFolders = ItemUtils.copyItems(this.childFolders, intoFolder.childFolders)
        intoFolder.dataModels = ItemUtils.copyItems(this.dataModels, intoFolder.dataModels)
        intoFolder.terminologies = ItemUtils.copyItems(this.terminologies, intoFolder.terminologies)
        intoFolder.codeSets = ItemUtils.copyItems(this.codeSets, intoFolder.codeSets)
        intoFolder.classificationSchemes = ItemUtils.copyItems(this.classificationSchemes, intoFolder.classificationSchemes)
    }

    @Override
    Item shallowCopy() {
        Folder folderShallowCopy = new Folder()
        this.copyInto(folderShallowCopy)
        return folderShallowCopy
    }
    Terminology terminology(Terminology terminology) {
        this.terminologies.add(terminology)
        terminology.folder = this
        terminology.parent = this
        terminology
    }

    Terminology terminology(Map args, @DelegatesTo(value = Terminology, strategy = Closure.DELEGATE_FIRST) Closure closure = {}) {
        Terminology terminology1 = Terminology.build(args + [parent: this, folder: this], closure)
        terminology terminology1
    }

    Terminology terminology(@DelegatesTo(value = Terminology, strategy = Closure.DELEGATE_FIRST) Closure closure = {}) {
        terminology [:], closure
    }

    CodeSet codeSet(CodeSet codeSet) {
        this.codeSets.add(codeSet)
        codeSet.folder = this
        codeSet.parent = this
        codeSet
    }

    CodeSet codeSet(Map args, @DelegatesTo(value = CodeSet, strategy = Closure.DELEGATE_FIRST) Closure closure = {}) {
        CodeSet codeSet1 = CodeSet.build(args + [parent: this, folder: this], closure)
        codeSet codeSet1
    }

    CodeSet codeSet(@DelegatesTo(value = CodeSet, strategy = Closure.DELEGATE_FIRST) Closure closure = {}) {
        codeSet [:], closure
    }

    DataModel dataModel(DataModel dataModel) {
        this.dataModels.add(dataModel)
        dataModel.folder = this
        dataModel.parent = this
        dataModel
    }

    DataModel dataModel(Map args, @DelegatesTo(value = DataModel, strategy = Closure.DELEGATE_FIRST) Closure closure = {}) {
        DataModel dataModel1 = DataModel.build(args + [parent: this, folder: this], closure)
        dataModel dataModel1
    }

    DataModel dataModel(@DelegatesTo(value = DataModel, strategy = Closure.DELEGATE_FIRST) Closure closure = {}) {
        dataModel [:], closure
    }


}