package uk.ac.ox.softeng.mauro.domain.model

import com.fasterxml.jackson.annotation.JsonIgnore
import groovy.transform.AutoClone
import groovy.transform.CompileStatic
import io.micronaut.core.annotation.Nullable
import io.micronaut.data.annotation.Relation
import jakarta.persistence.Transient
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import uk.ac.ox.softeng.mauro.domain.classifier.Classifier
import uk.ac.ox.softeng.mauro.domain.facet.Annotation
import uk.ac.ox.softeng.mauro.domain.facet.Metadata
import uk.ac.ox.softeng.mauro.domain.facet.ReferenceFile
import uk.ac.ox.softeng.mauro.domain.facet.SummaryMetadata
import uk.ac.ox.softeng.mauro.domain.security.CatalogueUser
import uk.ac.ox.softeng.mauro.exception.MauroInternalException

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
abstract class AdministeredItem extends Item {

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
     * The path of oan object allows it to be navigated to from either the containing model, or the folder path within
     * a system.  This value is calculated on persistence and saved to allow easy lookup.
     */
    @Transient
    Path path

    /**
     * The identifier of a breadcrumb tree object for navigation.
     */
    @Nullable
    UUID breadcrumbTreeId // should be BreadcrumbTree type

    @Relation(Relation.Kind.ONE_TO_MANY)
    List<Metadata> metadata = []

    @Relation(Relation.Kind.ONE_TO_MANY)
    List<SummaryMetadata> summaryMetadata = []


    @Relation(Relation.Kind.ONE_TO_MANY)
    List<Annotation> annotations = []

    @Relation(Relation.Kind.ONE_TO_MANY)
    List<ReferenceFile> referenceFiles = []

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
        parent.owner
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
    String getPathModelIdentifier() {
        null
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
            if (node.owner == node) break // root of Path is the owner of the item
            i++; node = node.parent
            if (i > Path.PATH_MAX_NODES) throw new MauroInternalException("Path exceeded maximum depth of [$Path.PATH_MAX_NODES]")
        }

        path = new Path()
        path.nodes = pathNodes
        path
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
    AdministeredItem clone()
    {
        AdministeredItem cloned = super.clone() as AdministeredItem

        List<Metadata> clonedMetadata = getMetadata().collect { it.clone() }
        cloned.metadata = clonedMetadata

        List<Annotation> clonedAnnotations = getAnnotations().collect {
            it.clone().tap {clonedAnnotation ->
                List<Annotation> clonedChildren = clonedAnnotation.childAnnotations.collect {child ->
                    child.clone()}
                clonedAnnotation.childAnnotations = clonedChildren
            }
        }
        cloned.annotations = clonedAnnotations

        List<SummaryMetadata> clonedSummaryMetadata = getSummaryMetadata().collect{it.clone()}
        cloned.summaryMetadata = clonedSummaryMetadata

        List<ReferenceFile> clonedReferenceFiles = getReferenceFiles().collect {it.clone()}
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
}
