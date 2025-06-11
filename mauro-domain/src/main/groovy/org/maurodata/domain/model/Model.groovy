package org.maurodata.domain.model

import com.fasterxml.jackson.annotation.JsonAlias
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
abstract class Model<M extends DiffableItem> extends AdministeredItem implements SecurableResource {

    public static final String DEFAULT_BRANCH_NAME = 'main'

    Boolean finalised = false

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
    String branchName = DEFAULT_BRANCH_NAME

    @Nullable
    //@Transient
    ModelVersion modelVersion

    @Nullable
    String modelVersionTag

    @JsonIgnore
    public boolean versionableFlag =true

    @Transient
    boolean isVersionable()
    {
        final List<Model> myAncestors= getAncestors()
        Collections.reverse(myAncestors)

        final Iterator<Model> models=myAncestors.iterator()
        while(models.hasNext())
        {
            final Model ancestor=models.next()
            if(ancestor instanceof Folder)
            {
                if(ancestor.@versionableFlag)
                {
                    return false
                }
            }
        }
        return versionableFlag
    }

    @Transient
    void setVersionable(final boolean versionable)
    {
        this.versionableFlag=versionable
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
    String getPathModelIdentifier() {
        final Model model=getModelWithVersion()

        if(model!=null)
        {
            return model.@modelVersion!=null ? model.@modelVersion.toString() : model.@branchName
        }

        return this.@modelVersion!=null ? this.@modelVersion.toString() : this.@branchName
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
    abstract void setAssociations()

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
    List<Model> getAncestors()
    {
        final ArrayList<Model> myAncestors=new ArrayList<>(3)

        final Model myParent=getParent()
        if(myParent==null){return myAncestors}

        myAncestors.add(myParent)
        myAncestors.addAll(myParent.getAncestors())

        return myAncestors
    }

    @JsonIgnore
    @Transient
    Model getModelWithVersion()
    {
        final List<Model> myAncestors= getAncestors()
        Collections.reverse(myAncestors)

        // Ignoring folders that are not versionable, find an ancestor
        // that has a version

        final Iterator<Model> it=myAncestors.iterator()
        while(it.hasNext())
        {
            final Model model = it.next()
            if(model instanceof Folder)
            {
                if(!model.@versionableFlag){continue}
            }
            if(model.@modelVersion!=null)
            {
                return model
            }
        }
        return this
    }

    ModelVersion getModelVersion()
    {
        return getModelWithVersion().@modelVersion
    }

    String getBranchName()
    {
        return getModelWithVersion().@branchName
    }

    String getModelVersionTag()
    {
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
    List<VersionLink> versionLinks=[]
}
