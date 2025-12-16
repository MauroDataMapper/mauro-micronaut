package org.maurodata.domain.model

import org.maurodata.domain.datamodel.DataModel

import com.fasterxml.jackson.annotation.JsonIgnore
import groovy.transform.CompileStatic
import groovy.transform.NamedParams
import groovy.util.logging.Slf4j
import io.micronaut.core.annotation.Nullable
import io.micronaut.data.annotation.Relation
import jakarta.persistence.Transient
import org.maurodata.domain.authority.Authority
import org.maurodata.domain.diff.CollectionDTO
import org.maurodata.domain.diff.DiffBuilder
import org.maurodata.domain.diff.DiffableItem
import org.maurodata.domain.diff.ObjectDiff
import org.maurodata.domain.facet.VersionLink
import org.maurodata.domain.folder.Folder
import org.maurodata.domain.model.version.ModelVersion
import org.maurodata.domain.security.SecurableResource

import java.time.Instant

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
@Slf4j
abstract class Model<M extends DiffableItem> extends AdministeredItem implements SecurableResource, ItemReferencer {

    public static final String DEFAULT_BRANCH_NAME = 'main'

    Boolean finalised = false

    @Transient
    @JsonIgnore
    Boolean getMyFinalised() {
        return this.@finalised
    }

    Boolean getFinalised() {
        Model modelWithVersion = getModelWithVersion()
        if (modelWithVersion != null) {return modelWithVersion.@finalised}
        return this.@finalised
    }

    @Nullable
    Instant dateFinalised

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
    protected String branchName = DEFAULT_BRANCH_NAME

    @Nullable
    //@Transient
    ModelVersion modelVersion

    @Nullable
    String modelVersionTag

    @JsonIgnore
    public boolean versionableFlag = true

    @Transient
    boolean isVersionable() {
        final List<Model> myAncestors = getAncestors()
        Collections.reverse(myAncestors)

        final Iterator<Model> models = myAncestors.iterator()
        while (models.hasNext()) {
            final Model ancestor = models.next()
            if (ancestor instanceof Folder) {
                if (ancestor.@versionableFlag) {
                    return false
                }
            }
        }
        return versionableFlag
    }

    @Transient
    void setVersionable(final boolean versionable) {
        this.versionableFlag = versionable
    }

    @Override
    @Transient
    @JsonIgnore
    Model getParent() {
        folder
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
    @Nullable
    String getPathModelIdentifier() {
        final Model model = getModelWithVersion()

        if (model != null) {
            return model.@modelVersion != null ? model.@modelVersion.toString() : model.@branchName
        }

        return this.@modelVersion != null ? this.@modelVersion.toString() : this.@branchName
    }

    /**
     * For a model which has all its associations loaded, return a collection of all its direct child items.
     * The collection should be ordered so that all the items can be saved in order.
     */
    @Transient
    @JsonIgnore
    List<AdministeredItem> getAllContents() {
        getAllAssociations().flatten() as List<AdministeredItem>
    }

    @Transient
    @JsonIgnore
    void setAssociations() {
        super.setAssociations()
        versionLinks.each {
            it.multiFacetAwareItem = this
        }
    }

    Boolean hasSameDomainType(Model other) {
        boolean result = this.domainType == other.domainType && this.class == other.class
        result
    }


    ObjectDiff diff(Model other) {
        if (!this.hasSameDomainType(other)) {
            log.warn("Unable to diff this object domainType: ${this.domainType} with : ${other.domainType}")
            return null
        }
        CollectionDTO lhs = DiffBuilder.createCollectionDiff(DiffBuilder.MODEL_COLLECTION_KEYS, this.properties)
        CollectionDTO rhs = DiffBuilder.createCollectionDiff(DiffBuilder.MODEL_COLLECTION_KEYS, other.properties)

        ObjectDiff baseDiff = DiffBuilder.diff(this, other, lhs, rhs)
        baseDiff
    }

    @JsonIgnore
    @Transient
    List<Model> getAncestors() {
        final ArrayList<Model> myAncestors = new ArrayList<>(3)

        final Model myParent = getParent()
        if (myParent == null) {return myAncestors}

        myAncestors.add(myParent)
        myAncestors.addAll(myParent.getAncestors())

        return myAncestors
    }

    @JsonIgnore
    @Transient
    Model getModelWithVersion() {
        final List<Model> myAncestors = getAncestors()
        Collections.reverse(myAncestors)

        // Ignoring folders that are not versionable, find an ancestor
        // that has a version
        for (int m = 0; m < myAncestors.size(); m++) {
            final Model model = myAncestors.get(m)
            if ((model instanceof Folder || model instanceof DataModel) && model.@versionableFlag) {
                return model
            }
        }
        return this
    }

    @JsonIgnore
    @Transient
    ModelVersion getMyModelVersion() {
        return this.@modelVersion
    }

    ModelVersion getModelVersion() {
        return getModelWithVersion().@modelVersion
    }

    void setBranchName(final String branchNameString) {
        this.@branchName = branchNameString
    }

    @JsonIgnore
    @Transient
    String getMyBranchName() {
        return this.@branchName
    }

    String getBranchName() {
        return getModelWithVersion().@branchName
    }

    @JsonIgnore
    @Transient
    String getMyModelVersionTag() {
        return this.@modelVersionTag
    }

    String getModelVersionTag() {
        return getModelWithVersion().@modelVersionTag
    }

    /****
     * Methods for building a tree-like DSL
     */

    Boolean finalised(Boolean finalised) {
        this.finalised = finalised
        this.finalised
    }

    Instant dateFinalised(Instant dateFinalised) {
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
        @DelegatesTo(value = ModelVersion, strategy = Closure.DELEGATE_FIRST) Closure closure = {}) {
        this.modelVersion = ModelVersion.build(args, closure)
        this.modelVersion
    }

    ModelVersion modelVersion(
        @DelegatesTo(value = ModelVersion, strategy = Closure.DELEGATE_FIRST) Closure closure = {}) {
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

    Folder folder(Folder folder) {
        this.folder = folder
        folder
    }

    /* VersionLinkAware */

    @Relation(Relation.Kind.ONE_TO_MANY)
    List<VersionLink> versionLinks = []

    @Transient
    @JsonIgnore
    @Override
    List<ItemReference> retrieveItemReferences() {
        List<ItemReference> pathsBeingReferenced = [] + super.retrieveItemReferences()

        ItemReferencerUtils.addItem(folder, pathsBeingReferenced)
        ItemReferencerUtils.addItem(authority, pathsBeingReferenced)

        return pathsBeingReferenced
    }

    @Transient
    @JsonIgnore
    @Override
    void replaceItemReferencesByIdentity(IdentityHashMap<Item, Item> replacements, List<Item> notReplaced) {
        super.replaceItemReferencesByIdentity(replacements, notReplaced)
        folder = ItemReferencerUtils.replaceItemByIdentity(folder, replacements, notReplaced)
        authority = ItemReferencerUtils.replaceItemByIdentity(authority, replacements, notReplaced)
    }

    @Override
    void copyInto(Item into) {
        super.copyInto(into)
        Model intoModel = (Model) into
        intoModel.finalised = ItemUtils.copyItem(this.getMyFinalised(), intoModel.getMyFinalised())
        intoModel.dateFinalised = ItemUtils.copyItem(this.dateFinalised, intoModel.dateFinalised)
        intoModel.documentationVersion = ItemUtils.copyItem(this.documentationVersion, intoModel.documentationVersion)
        intoModel.readableByEveryone = ItemUtils.copyItem(this.readableByEveryone, intoModel.readableByEveryone)
        intoModel.readableByAuthenticatedUsers = ItemUtils.copyItem(this.readableByAuthenticatedUsers, intoModel.readableByAuthenticatedUsers)
        intoModel.modelType = ItemUtils.copyItem(this.modelType, intoModel.modelType)
        intoModel.organisation = ItemUtils.copyItem(this.organisation, intoModel.organisation)
        intoModel.deleted = ItemUtils.copyItem(this.deleted, intoModel.deleted)
        intoModel.author = ItemUtils.copyItem(this.author, intoModel.author)
        intoModel.branchName = ItemUtils.copyItem(this.getMyBranchName(), intoModel.getMyBranchName())
        intoModel.folder = ItemUtils.copyItem(this.folder, intoModel.folder)
        intoModel.authority = ItemUtils.copyItem(this.authority, intoModel.authority)
        intoModel.modelVersion = ItemUtils.copyItem(this.getMyModelVersion(), intoModel.getMyModelVersion())
        intoModel.modelVersionTag = ItemUtils.copyItem(this.getMyModelVersionTag(), intoModel.getMyModelVersionTag())
        intoModel.versionableFlag = ItemUtils.copyItem(this.versionableFlag, intoModel.versionableFlag)
        intoModel.versionLinks = ItemUtils.copyItems(this.versionLinks, intoModel.versionLinks)
    }
}
