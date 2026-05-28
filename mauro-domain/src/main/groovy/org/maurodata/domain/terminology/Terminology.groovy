package org.maurodata.domain.terminology

import jakarta.persistence.PrePersist
import jakarta.persistence.PreUpdate
import org.maurodata.domain.diff.DiffBuilder
import org.maurodata.domain.diff.DiffableItem
import org.maurodata.domain.diff.ObjectDiff
import org.maurodata.domain.model.Item
import org.maurodata.domain.model.ItemReference
import org.maurodata.domain.model.ItemReferencer
import org.maurodata.domain.model.ItemReferencerUtils
import org.maurodata.domain.model.ItemUtils
import org.maurodata.util.DedupingObjectIdResolver

import com.fasterxml.jackson.annotation.JsonIdentityInfo
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonPropertyOrder
import com.fasterxml.jackson.annotation.ObjectIdGenerators
import groovy.transform.AutoClone
import groovy.transform.CompileStatic
import groovy.transform.MapConstructor
import groovy.util.logging.Slf4j
import io.micronaut.core.annotation.Introspected
import io.micronaut.data.annotation.Index
import io.micronaut.data.annotation.Indexes
import io.micronaut.data.annotation.MappedEntity
import io.micronaut.data.annotation.Relation
import jakarta.persistence.Transient
import org.maurodata.domain.model.Model
import org.maurodata.domain.model.ModelItem

/**
 * A Terminology is a model that describes a number of terms, and some relationships between them.
 */
@Slf4j
@CompileStatic
@AutoClone
@Introspected
@MappedEntity(schema = 'terminology')
@MapConstructor(includeSuperFields = true, includeSuperProperties = true, noArg = true)
@Indexes([@Index(columns = ['folder_id', 'label', 'branch_name', 'model_version'], unique = true)])
@JsonPropertyOrder(['terms', 'termRelationshipTypes'])
class Terminology extends Model implements ItemReferencer, DiffableItem<Terminology> {

    @Relation(value = Relation.Kind.ONE_TO_MANY, mappedBy = 'terminology')
    @JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator, property = 'code', scope = Term, resolver = DedupingObjectIdResolver)
    List<Term> terms = []

    @Relation(value = Relation.Kind.ONE_TO_MANY, mappedBy = 'terminology')
    @JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator, property = 'label', scope = TermRelationshipType, resolver = DedupingObjectIdResolver)
    List<TermRelationshipType> termRelationshipTypes = []

    @Relation(value = Relation.Kind.ONE_TO_MANY, mappedBy = 'terminology')
    List<TermRelationship> termRelationships = []

    @Override
    @Transient
    @JsonIgnore
    String getPathPrefix() {
        'te'
    }

    @Override
    @Transient
    @JsonIgnore
    List<List<ModelItem<Terminology>>> getAllAssociations() {
        [terms, termRelationshipTypes, termRelationships] as List<List<ModelItem<Terminology>>>
    }

    @Transient
    @JsonIgnore
    @Override
    void setAssociations() {
        super.setAssociations()
        Map<UUID, Term> termsMap = terms.collectEntries {[it.id?:it.code, it]}
        Map<UUID, TermRelationshipType> termRelationshipTypesMap = termRelationshipTypes.collectEntries {[it.id?:it.label, it]}

        terms.each {
            it.parent = this
        }
        termRelationshipTypes.each {
            it.parent = this
        }
        termRelationships.each {
            it.parent = this
            it.relationshipType = termRelationshipTypesMap[it.relationshipType.id?:it.relationshipType.label]
            it.sourceTerm = termsMap[it.sourceTerm.id?:it.sourceTerm.code]
            it.targetTerm = termsMap[it.targetTerm.id?:it.targetTerm.code]
        }
    }

    @Override
    Terminology clone() {
        log.debug '*** Terminology.clone() ***'

        Terminology cloned = (Terminology) super.clone()
        cloned.terms = cloned.terms.collect {it.clone()}
        cloned.termRelationshipTypes = cloned.termRelationshipTypes.collect {it.clone()}
        cloned.termRelationships = cloned.termRelationships.collect {it.clone()}

        cloned.setAssociations()

        cloned
    }

    @PrePersist
    @PreUpdate
    void prePersist() {
        super.prePersist()
        if (!getModelWithVersion()) {
            branchName = 'main'
        }
    }

    @Override
    @JsonIgnore
    @Transient
    ObjectDiff<Terminology> diff(Terminology other, String lhsPathRoot, String rhsPathRoot) {
        ObjectDiff<Terminology> base = DiffBuilder.objectDiff(Terminology)
            .leftHandSide(id?.toString(), this)
            .rightHandSide(other.id?.toString(), other)
        base.label = this.label
        base.appendString(DiffBuilder.DESCRIPTION, this.description, other.description, this, other)
        base.appendString(DiffBuilder.ALIASES_STRING, this.aliasesString, other.aliasesString, this, other)
        if (!DiffBuilder.isNullOrEmpty(this.terms as Collection<Object>) || !DiffBuilder.isNullOrEmpty(other.terms as Collection<Object>)) {
            base.appendCollection(DiffBuilder.TERMS, this.terms as Collection<DiffableItem>, other.terms as Collection<DiffableItem>, lhsPathRoot,
                                  rhsPathRoot)
        }
        if (!DiffBuilder.isNullOrEmpty(this.termRelationshipTypes as Collection<Object>) || !DiffBuilder.isNullOrEmpty(other.termRelationshipTypes as Collection<Object>)) {
            base.appendCollection(DiffBuilder.TERM_RELATIONSHIP_TYPES, this.termRelationshipTypes as Collection<DiffableItem>, other.termRelationshipTypes as Collection<DiffableItem>, lhsPathRoot,
                                  rhsPathRoot)
        }
        if (!DiffBuilder.isNullOrEmpty(this.termRelationships as Collection<Object>) || !DiffBuilder.isNullOrEmpty(other.termRelationships as Collection<Object>)) {
            base.appendCollection(DiffBuilder.TERM_RELATIONSHIPS, this.termRelationships as Collection<DiffableItem>, other.termRelationships as Collection<DiffableItem>, lhsPathRoot,
                                  rhsPathRoot)
        }
        base
    }


    /****
     * Methods for building a tree-like DSL
     */

    static Terminology build(
        Map args,
        @DelegatesTo(value = Terminology, strategy = Closure.DELEGATE_FIRST) Closure closure = {}) {
        new Terminology(args).tap(closure)
    }

    static Terminology build(
        @DelegatesTo(value = Terminology, strategy = Closure.DELEGATE_FIRST) Closure closure = {}) {
        build [:], closure
    }

    Term term(Term term) {
        this.terms.add(term)
        term.terminology = this
        term
    }

    Term term(Map args, @DelegatesTo(value = Term, strategy = Closure.DELEGATE_FIRST) Closure closure = {}) {
        Term t = Term.build(args, closure)
        t.terminology = this
        this.terms.add(t)
        t
    }

    Term term(@DelegatesTo(value = Term, strategy = Closure.DELEGATE_FIRST) Closure closure = {}) {
        term [:], closure
    }

    TermRelationshipType termRelationshipType(TermRelationshipType termRelationshipType) {
        this.termRelationshipTypes.add(termRelationshipType)
        termRelationshipType.terminology = this
        termRelationshipType
    }

    TermRelationshipType termRelationshipType(
        Map args,
        @DelegatesTo(value = TermRelationshipType, strategy = Closure.DELEGATE_FIRST) Closure closure = {}) {
        TermRelationshipType termRelationshipType = TermRelationshipType.build(args, closure)
        termRelationshipType.terminology = this
        this.termRelationshipTypes.add(termRelationshipType)
        termRelationshipType
    }

    TermRelationshipType termRelationshipType(
        @DelegatesTo(value = TermRelationshipType, strategy = Closure.DELEGATE_FIRST) Closure closure = {}) {
        termRelationshipType [:], closure
    }

    TermRelationship termRelationship(TermRelationship termRelationship) {
        this.termRelationships.add(termRelationship)
        termRelationship.terminology = this
        termRelationship
    }

    TermRelationship termRelationship(
        Map args,
        @DelegatesTo(value = TermRelationship, strategy = Closure.DELEGATE_FIRST) Closure closure = {}) {
        TermRelationship termRelationship = TermRelationship.build(args)
        termRelationship.terminology = this
        this.termRelationships.add(termRelationship)
        termRelationship.tap(closure)
    }

    TermRelationship termRelationship(
        @DelegatesTo(value = TermRelationship, strategy = Closure.DELEGATE_FIRST) Closure closure = {}) {
        termRelationship [:], closure
    }

    @Transient
    @JsonIgnore
    @Override
    List<ItemReference> retrieveItemReferences() {
        List<ItemReference> pathsBeingReferenced = [] + super.retrieveItemReferences()

        ItemReferencerUtils.addItems(terms, pathsBeingReferenced)
        ItemReferencerUtils.addItems(termRelationshipTypes, pathsBeingReferenced)
        ItemReferencerUtils.addItems(termRelationships, pathsBeingReferenced)

        return pathsBeingReferenced
    }

    @Transient
    @JsonIgnore
    @Override
    void replaceItemReferencesByIdentity(IdentityHashMap<Item, Item> replacements, Map<UUID, Item> allItemsById, List<Item> notReplaced) {
        super.replaceItemReferencesByIdentity(replacements, allItemsById, notReplaced)
        terms = ItemReferencerUtils.replaceItemsByIdentity(terms, replacements, notReplaced)
        termRelationshipTypes = ItemReferencerUtils.replaceItemsByIdentity(termRelationshipTypes, replacements, notReplaced)
        termRelationships = ItemReferencerUtils.replaceItemsByIdentity(termRelationships, replacements, notReplaced)
    }

    @Override
    void copyInto(Item into) {
        super.copyInto(into)
        Terminology intoTerminology = (Terminology) into
        intoTerminology.terms = ItemUtils.copyItems(this.terms, intoTerminology.terms)
        intoTerminology.termRelationshipTypes = ItemUtils.copyItems(this.termRelationshipTypes, intoTerminology.termRelationshipTypes)
        intoTerminology.termRelationships = ItemUtils.copyItems(this.termRelationships, intoTerminology.termRelationships)
    }

    @Override
    Item shallowCopy() {
        Terminology terminologyShallowCopy = new Terminology()
        this.copyInto(terminologyShallowCopy)
        return terminologyShallowCopy
    }
}
