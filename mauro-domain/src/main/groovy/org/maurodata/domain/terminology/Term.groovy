package org.maurodata.domain.terminology

import org.maurodata.domain.diff.BaseCollectionDiff
import org.maurodata.domain.diff.CollectionDiff
import org.maurodata.domain.diff.DiffBuilder
import org.maurodata.domain.diff.DiffableItem
import org.maurodata.domain.diff.ObjectDiff
import org.maurodata.domain.model.Item
import org.maurodata.domain.model.ItemReference
import org.maurodata.domain.model.ItemReferencer
import org.maurodata.domain.model.ItemReferencerUtils
import org.maurodata.domain.model.ItemUtils

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import groovy.transform.AutoClone
import groovy.transform.CompileStatic
import groovy.transform.MapConstructor
import io.micronaut.core.annotation.Introspected
import io.micronaut.core.annotation.Nullable
import io.micronaut.data.annotation.Index
import io.micronaut.data.annotation.Indexes
import io.micronaut.data.annotation.MappedEntity
import io.micronaut.data.annotation.Relation
import jakarta.persistence.Transient
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import org.maurodata.domain.model.AdministeredItem
import org.maurodata.domain.model.ModelItem

/**
 * A term describes a value with a code and a meaning, within the context of a terminology.
 * <p>
 * Relationships may be defined between terms, and they may be re-used as part of a codeset - a collection of terms
 * taken from one or more terminologies.
 *
 * @see Terminology
 */
@CompileStatic
@AutoClone(excludes = ['terminology', 'sourceTermRelationships', 'targetTermRelationships'])
@Introspected
@MappedEntity(schema = 'terminology')
@MapConstructor(includeSuperFields = true, includeSuperProperties = true, noArg = true)
@Indexes([@Index(columns = ['terminology_id', 'code'], unique = true)])
class Term extends ModelItem<Terminology> implements ItemReferencer, DiffableItem<Term> {

    @Override
    String getLabel() {
        code && definition && code == definition ? code :
        code && definition ? "${code}: ${definition}" :
        code
    }

    @JsonIgnore
    Terminology terminology

    @NotBlank
    String code

    @NotBlank
    String definition

    @Nullable
    String url

    @Nullable
    Boolean isParent

    @Nullable
    Integer depth

    @Relation(value = Relation.Kind.ONE_TO_MANY, mappedBy = 'sourceTerm')
    List<TermRelationship> sourceTermRelationships = []

    @Relation(value = Relation.Kind.ONE_TO_MANY, mappedBy = 'targetTerm')
    List<TermRelationship> targetTermRelationships = []

    @Transient
    @JsonIgnore
    List<List<TermRelationship>> getAllAssociations() {
        [sourceTermRelationships, targetTermRelationships]
    }

    @Relation(value = Relation.Kind.MANY_TO_MANY, mappedBy = 'terms')
    Set<CodeSet> codeSets = []


    @Override
    @Transient
    @JsonIgnore
    Terminology getParent() {
        terminology
    }

    @Override
    @Transient
    @JsonIgnore
    void setParent(AdministeredItem terminology) {
        this.terminology = (Terminology) terminology
    }

    @Override
    @Transient
    @JsonIgnore
    String getPathPrefix() {
        'tm'
    }

    @Override
    @Transient
    @JsonIgnore
    String getPathIdentifier() {
        code
    }

    @Transient
    @Deprecated
    @Nullable
    @JsonProperty('model')
    UUID getModelId() {
        terminology?.id
    }


    @Override
    @JsonIgnore
    @Transient
    String getDiffIdentifier() {
        if (terminology != null) {
            return "${terminology.diffIdentifier}|${getPathNodeString()}"
        }
        return "${getPathNodeString()}"
    }

    @Override
    @JsonIgnore
    @Transient
    ObjectDiff<Term> diff(Term other, String lhsPathRoot, String rhsPathRoot) {
        ObjectDiff<Term> base = DiffBuilder.objectDiff(Term)
            .leftHandSide(id?.toString(), this)
            .rightHandSide(other.id?.toString(), other)
        base.label = this.label
        base.appendString(DiffBuilder.DESCRIPTION, this.description, other.description, this, other)
        base.appendString(DiffBuilder.CODE, this.code, other.code, this, other)
        base.appendString(DiffBuilder.DEFINITION, this.definition, other.definition, this, other)
        base.appendString(DiffBuilder.URL, this.url, other.url, this, other)
        base.appendString(DiffBuilder.ALIASES_STRING, this.aliasesString, other.aliasesString, this, other)
        if (!DiffBuilder.isNullOrEmpty(this.sourceTermRelationships as Collection<Object>) || !DiffBuilder.isNullOrEmpty(other.sourceTermRelationships as Collection<Object>)) {
            base.appendCollection(DiffBuilder.SOURCE_TERM_RELATIONSHIPS, this.sourceTermRelationships as Collection<DiffableItem>, other.sourceTermRelationships as Collection<DiffableItem>, lhsPathRoot,
                                  rhsPathRoot)
        }
        if (!DiffBuilder.isNullOrEmpty(this.targetTermRelationships as Collection<Object>) || !DiffBuilder.isNullOrEmpty(other.targetTermRelationships as Collection<Object>)) {
            base.appendCollection(DiffBuilder.TARGET_TERM_RELATIONSHIPS, this.targetTermRelationships as Collection<DiffableItem>, other.targetTermRelationships as Collection<DiffableItem>, lhsPathRoot,
                                  rhsPathRoot)
        }
        base
    }


    /****
     * Methods for building a tree-like DSL
     */
    static Term build(
        Map args,
        @DelegatesTo(value = Term, strategy = Closure.DELEGATE_FIRST) Closure closure = {}) {
        new Term(args).tap(closure)
    }

    static Term build(@DelegatesTo(value = Term, strategy = Closure.DELEGATE_FIRST) Closure closure = {}) {
        build [:], closure
    }

    String code(String code) {
        this.code = code
    }

    String definition(String definition) {
        this.definition = definition
    }

    String url(String url) {
        this.url = url
    }

    Boolean isParent(Boolean isParent) {
        this.isParent = isParent
    }

    Integer depth(Integer depth) {
        this.depth = depth
    }

    @Override
    String toString() {
        return "Term{" +
               "terminology=" + terminology +
               ", code='" + code + '\'' +
               ", definition='" + definition + '\'' +
               ", url='" + url + '\'' +
               ", isParent=" + isParent +
               ", depth=" + depth +
               ", sourceTermRelationships=" + sourceTermRelationships +
               ", targetTermRelationships=" + targetTermRelationships +
               ", codeSets=" + codeSets +
               '}'
    }

    @Transient
    @JsonIgnore
    @Override
    List<ItemReference> retrieveItemReferences() {
        List<ItemReference> pathsBeingReferenced = [] + super.retrieveItemReferences()

        ItemReferencerUtils.addItems(sourceTermRelationships, pathsBeingReferenced)
        ItemReferencerUtils.addItems(targetTermRelationships, pathsBeingReferenced)
        ItemReferencerUtils.addItems(codeSets, pathsBeingReferenced)

        return pathsBeingReferenced
    }

    @Transient
    @JsonIgnore
    @Override
    void replaceItemReferencesByIdentity(IdentityHashMap<Item, Item> replacements, Map<UUID, Item> allItemsById, List<Item> notReplaced) {
        super.replaceItemReferencesByIdentity(replacements, allItemsById, notReplaced)
        terminology = ItemReferencerUtils.replaceItemByIdentity(terminology, replacements, notReplaced)
        sourceTermRelationships = ItemReferencerUtils.replaceItemsByIdentity(sourceTermRelationships, replacements, notReplaced)
        targetTermRelationships = ItemReferencerUtils.replaceItemsByIdentity(targetTermRelationships, replacements, notReplaced)
        codeSets = ItemReferencerUtils.replaceItemsByIdentity(codeSets, replacements, notReplaced)
    }

    @Override
    void copyInto(Item into) {
        super.copyInto(into)
        Term intoDataType = (Term) into
        intoDataType.terminology = ItemUtils.copyItem(this.terminology, intoDataType.terminology)
        intoDataType.code = ItemUtils.copyItem(this.code, intoDataType.code)
        intoDataType.definition = ItemUtils.copyItem(this.definition, intoDataType.definition)
        intoDataType.url = ItemUtils.copyItem(this.url, intoDataType.url)
        intoDataType.isParent = ItemUtils.copyItem(this.isParent, intoDataType.isParent)
        intoDataType.depth = ItemUtils.copyItem(this.depth, intoDataType.depth)
        intoDataType.sourceTermRelationships = ItemUtils.copyItems(this.sourceTermRelationships, intoDataType.sourceTermRelationships)
        intoDataType.targetTermRelationships = ItemUtils.copyItems(this.targetTermRelationships, intoDataType.targetTermRelationships)
        intoDataType.codeSets = ItemUtils.copyItems(this.codeSets, intoDataType.codeSets)
        // depends on code and definition
        intoDataType.label = ItemUtils.copyItem(this.label, intoDataType.label)
    }

    @Override
    Item shallowCopy() {
        Term termShallowCopy = new Term()
        this.copyInto(termShallowCopy)
        return termShallowCopy
    }
}
