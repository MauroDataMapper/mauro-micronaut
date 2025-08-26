package org.maurodata.domain.model

import org.maurodata.domain.classifier.Classifier
import org.maurodata.domain.facet.Annotation
import org.maurodata.domain.facet.Edit
import org.maurodata.domain.facet.Facet
import org.maurodata.domain.facet.Metadata
import org.maurodata.domain.facet.ReferenceFile
import org.maurodata.domain.facet.Rule
import org.maurodata.domain.facet.SemanticLink
import org.maurodata.domain.facet.SummaryMetadata
import org.maurodata.domain.security.CatalogueUser
import org.maurodata.exception.MauroInternalException
import org.maurodata.profile.ProfileField

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonInclude
import groovy.transform.AutoClone
import groovy.transform.CompileStatic
import io.micronaut.core.annotation.Nullable
import io.micronaut.data.annotation.Relation
import jakarta.persistence.Transient
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern

import java.time.Instant

/**
 * An AdministeredItem is an item stored in the catalogue.
 * <p>
 * A certain amount of administrative metadata is stored for each item - for example its identifier (UUID), the
 * date/time it was created, and the date/time it was last updated.
 * Every item has, by default, a String-valued label, and a longer description (although some administered items may
 * use other fields for these purposes.
 */

@CompileStatic
@AutoClone
abstract class AdministeredItem extends Item implements Pathable {

    /**
     * The label of an object.  Labels are used as identifiers within a context and so need to be unique within
     * that context. Labels of models must be unique when combined with the version number or branch name.
     * <p>
     *     A label cannot contain the characters `$`, `|` or `@`, since this values are used in the creation of paths.
     */
    @NotBlank
    @Pattern(regexp = /[^\$@|]*/, message = 'Cannot contain $, | or @')
    String label

    /**
     * The textual description of an object.  Descriptions can be formatted in plain text, markdown or HTML.
     */
    @Nullable
    String description

    /**
     * A list of other names for this object, separated by semi-colons.  These do not have to be unique.
     */
    @Nullable
    String aliasesString


    @Transient
    void setAliases(List<String> aliases) {
        aliasesString = aliases?.join(";")
    }

    @Transient
    List<String> getAliases() {
        aliasesString?.split(";") as List
    }

    @Transient
    @Relation(Relation.Kind.ONE_TO_MANY)
    List<Classifier> classifiers = []


    /**
     * The path of an object allows it to be navigated to from either the containing model, or the folder path within
     * a system.  This value is calculated on persistence and saved to allow easy lookup.
     */
    @Transient
    Path path

    /**
     * A different representation of the item's path.
     */
    @Transient
    List<Breadcrumb> breadcrumbs

    /**
     * The identifier of a breadcrumb tree object for navigation.
     */
    @Nullable
    UUID breadcrumbTreeId // should be BreadcrumbTree type

    @Relation(Relation.Kind.ONE_TO_MANY)
    List<Edit> edits = []

    @Relation(Relation.Kind.ONE_TO_MANY)
    List<Metadata> metadata = []

    @Relation(Relation.Kind.ONE_TO_MANY)
    List<SummaryMetadata> summaryMetadata = []

    @Relation(Relation.Kind.ONE_TO_MANY)
    List<Rule> rules = []

    @Relation(Relation.Kind.ONE_TO_MANY)
    List<Annotation> annotations = []

    @Relation(Relation.Kind.ONE_TO_MANY)
    List<ReferenceFile> referenceFiles = []

    @Transient
    List<String> availableActions = []

    @Relation(Relation.Kind.ONE_TO_MANY)
    List<SemanticLink> semanticLinks = []

    /**
     * Helper method for returning the parent of this object, if one exists and is loaded.
     * <p>
     *     The parent of an object is its direct ancestor in its path.
     */
    @Transient
    @JsonIgnore
    abstract AdministeredItem getParent()

    /**
     * Helper method for setting the parent of this object.
     */
    @Transient
    @JsonIgnore
    abstract void setParent(AdministeredItem parent)

    /**
     * Helper method for returning the owner of this object.
     * <p>
     *     The owner of an object is the root of an item's path, and is where permissions are set.
     */
    @Transient
    @JsonIgnore
    Model getOwner() {
        parent?.owner
    }

    /**
     * The prefix used in this item's Path, if this item is pathable.
     */
    @Transient
    @JsonIgnore
    String getPathPrefix() {
        null
    }

    /**
     * The identifier (label) used in this item's Path, if this item is pathable.
     */
    @Transient
    @JsonIgnore
    String getPathIdentifier() {
        this.label
    }

    /**
     * The model identifier (version string or branch label) used in this item's Path, if this item is pathable.
     */
    @Transient
    @JsonIgnore
    @Override
    @Nullable
    String getPathModelIdentifier() {
        return null
    }

    /**
     * Get this item's PathNode String
     */
    @Transient
    @JsonIgnore
    String getPathNodeString() {
        (new Path.PathNode(prefix: this.pathPrefix, identifier: this.pathIdentifier, modelIdentifier: this.pathModelIdentifier)).toString()
    }

    @JsonIgnore
    @Transient
    String getDiffIdentifier() {
        if (parent != null) {return "${parent.getDiffIdentifier()}|${this.pathNodeString}"}
        return pathNodeString
    }

    /**
     * Recalculate this item's Path from its parents. This item must have all its parent items loaded.
     *
     * @return The new Path
     */
    Path updatePath() {
        if (!pathPrefix) throw new MauroInternalException("Class [${this.class.simpleName}] is not Pathable")
        List<Path.PathNode> pathNodes = []
        int i = 0
        AdministeredItem node = this
        while (node) {
            pathNodes.add(0, new Path.PathNode(prefix: node.pathPrefix, identifier: node.pathIdentifier, modelIdentifier: node.pathModelIdentifier))
            if (node.parent == node) break // disallow cycles
            i++; node = node.parent
            if (i > Path.PATH_MAX_NODES) throw new MauroInternalException("Path exceeded maximum depth of [$Path.PATH_MAX_NODES]")
        }

        path = new Path(pathNodes)
        return path
    }

    /**
     * Recalculate this item's Path from its parents, all the way out to the edge of space
     * This item must have all its parent items loaded. ( via pathRepository.readParentItems() )
     *
     * @return The new Path
     */
    @Transient
    @JsonIgnore
    Path getPathToEdge() {
        if (!pathPrefix) throw new MauroInternalException("Class [${this.class.simpleName}] is not Pathable")
        List<Path.PathNode> pathNodes = []
        int i = 0
        AdministeredItem node = this
        while (node) {
            pathNodes.add(0, new Path.PathNode(prefix: node.pathPrefix, identifier: node.pathIdentifier, modelIdentifier: node.pathModelIdentifier, node: node))
            i++; node = node.parent
            if (i > Path.PATH_MAX_NODES) throw new MauroInternalException("Path exceeded maximum depth of [$Path.PATH_MAX_NODES]")
        }

        Path pathToEdge = new Path()
        pathToEdge.nodes = pathNodes

        return pathToEdge
    }

    /**
     * Recalculate this item's breadcrumbs from its parents. This item must have all its parent items loaded.
     * @return The new breadcrumbs
     */
    List<Breadcrumb> updateBreadcrumbs() {
        List<Breadcrumb> breadcrumbs = []
        int i = 0
        AdministeredItem node = this
        while (node) {
            breadcrumbs.add(new Breadcrumb(id: node.id, domainType: node.domainType, label: node.label, finalised: node instanceof Model ? node.finalised : null))
            if (node.parent == node || node instanceof Model) break
            // root of Breadcrumbs is the nearest Model type parent of the item
            i++; node = node.parent
            if (i > Path.PATH_MAX_NODES) throw new MauroInternalException("Breadcrumbs exceeded maximum depth of [$Path.PATH_MAX_NODES]")
        }

        this.breadcrumbs = breadcrumbs.tail().reverse()
        this.breadcrumbs
    }

    @Transient
    @JsonInclude(JsonInclude.Include.NON_NULL)
    List<ProfileField> profileFields

    /* Helper method for dealing with metadata across different namespaces:
        Given a namespace, return the metadata matching that namespace but as a map from keys to values
     */

    Map<String, String> metadataAsMap(String namespace) {
        getMetadata().findAll { it.namespace == namespace }
                .collectEntries { [it.key, it.value] }
    }


    /**
     * Return all persistent associations of the AdministeredItem.
     * The associations should be ordered so that they can be saved in order.
     */
    @Transient
    @JsonIgnore
    List<Collection<AdministeredItem>> getAllAssociations() {
        []
    }

    @Transient
    @JsonIgnore
    @Override
    AdministeredItem clone() {
        AdministeredItem cloned = super.clone() as AdministeredItem

        List<Metadata> clonedMetadata = getMetadata().collect { it.clone() }
        cloned.metadata = clonedMetadata

        List<Annotation> clonedAnnotations = getAnnotations().collect {
            it.clone().tap {clonedAnnotation ->
                List<Annotation> clonedChildren = clonedAnnotation.childAnnotations?.collect {child ->
                    child.clone()
                }
                clonedAnnotation.childAnnotations = clonedChildren
            }
        }
        cloned.annotations = clonedAnnotations

        List<SummaryMetadata> clonedSummaryMetadata = getSummaryMetadata().collect { it.clone() }
        cloned.summaryMetadata = clonedSummaryMetadata

        List<ReferenceFile> clonedReferenceFiles = getReferenceFiles().collect { it.clone() }
        cloned.referenceFiles = clonedReferenceFiles
        cloned

    }

    /**
     * DSL helper method for setting the identifier.  Returns the identifier passed in.
     *
     * @see #id
     */
    UUID id(UUID id) {
        this.id = id
        this.id
    }

    /**
     * DSL helper method for setting the identifier.  Returns the identifier passed in.
     *
     * @see #id
     */
    UUID id(String id) {
        this.id = UUID.fromString(id)
        this.id
    }

    /**
     * DSL helper method for setting the label.  Returns the label passed in.
     *
     * @see #label
     */
    String label(String label) {
        this.label = label
        this.label
    }

    /**
     * DSL helper method for setting the date created.  Returns the date/time passed in.
     *
     * @see #dateCreated
     */
    Instant dateCreated(Instant dateCreated) {
        this.dateCreated = dateCreated
        this.dateCreated
    }

    /**
     * DSL helper method for setting the date created.  Returns the date/time passed in.
     *
     * @see #dateCreated
     */
    Instant dateCreated(String dateCreated) {
        this.dateCreated = Instant.parse(dateCreated)
        this.dateCreated
    }

    /**
     * DSL helper method for setting the date this object was last updated.  Returns the date/time passed in.
     *
     * @see #lastUpdated
     */
    Instant lastUpdated(Instant lastUpdated) {
        this.lastUpdated = lastUpdated
        this.lastUpdated
    }

    /**
     * DSL helper method for setting the date this object was last updated.  Returns the date/time passed in.
     *
     * @see #lastUpdated
     */
    Instant lastUpdated(String lastUpdated) {
        this.lastUpdated = Instant.parse(lastUpdated)
        this.lastUpdated
    }

    /**
     * DSL helper method for setting the description.  Returns the description passed in.
     *
     * @see #description
     */
    String description(String description) {
        this.description = description
        this.description
    }

    /**
     * DSL helper method for setting the aliases string.  Returns the aliases string passed in.
     *
     * @see #aliasesString
     */
    String aliasesString(String aliasesString) {
        this.aliasesString = aliasesString
        this.aliasesString
    }

    /**
     * DSL helper method for setting the createdBy field.  Returns the createdBy user passed in.
     *
     * @see #catalogueUser
     */
    CatalogueUser createdBy(CatalogueUser createdBy) {
        this.catalogueUser = createdBy
        this.catalogueUser
    }

    /**
     * DSL helper method for adding to the metadata field.  Returns the newly-created metadata item.
     *
     * @see #metadata
     */
    Metadata metadata(String namespace, String key, String value) {
        Metadata md = Metadata.build(namespace: namespace, key: key, value: value)
        this.metadata.add(md)
        return md
    }

    /**
     * DSL helper method for adding to the metadata field.  Returns the metadata list passed in.
     *
     * @see #metadata
     */
    List<Metadata> metadata(List<Metadata> metadata) {
        this.metadata.addAll(metadata)
        return metadata
    }

    /**
     * DSL helper method for adding to the metadata field.  Returns the metadata list passed in.
     *
     * @see #metadata
     */
    List<Metadata> metadata(String namespace, Map<String, String> keyValueMap) {
        this.metadata.addAll(keyValueMap.collect { key, value ->
            new Metadata(namespace: namespace, key: key, value: value)
        })
        return metadata
    }

    /**
     * DSL helper method for adding to the summary metadata field.  Returns the summary metadata passed in.
     *
     * @see #metadata
     */
    SummaryMetadata summaryMetadata(SummaryMetadata summaryMetadata) {
        this.summaryMetadata.add(summaryMetadata)
        summaryMetadata
    }

    SummaryMetadata summaryMetadata(Map args, @DelegatesTo(value = SummaryMetadata, strategy = Closure.DELEGATE_FIRST) Closure closure = {}) {
        SummaryMetadata summaryMetadata1 = SummaryMetadata.build(args, closure)
        summaryMetadata summaryMetadata1
    }

    SummaryMetadata summaryMetadata(@DelegatesTo(value = SummaryMetadata, strategy = Closure.DELEGATE_FIRST) Closure closure = {}) {
        summaryMetadata [:], closure
    }

    /**
     * DSL helper method for adding to the summary metadata field.  Returns the summary metadata passed in.
     *
     * @see #metadata
     */
    Rule rule(Rule rule) {
        this.rules.add(rule)
        rule
    }

    Rule rule(Map args, @DelegatesTo(value = Rule, strategy = Closure.DELEGATE_FIRST) Closure closure = {}) {
        Rule rule1 = Rule.build(args, closure)
        rule rule1
    }

    Rule rule(@DelegatesTo(value = Rule, strategy = Closure.DELEGATE_FIRST) Closure closure = {}) {
        rule [:], closure
    }


}
