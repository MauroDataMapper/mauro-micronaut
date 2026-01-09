package org.maurodata.domain.terminology

import org.maurodata.domain.model.Item
import org.maurodata.domain.model.ItemReference
import org.maurodata.domain.model.ItemReferencer
import org.maurodata.domain.model.ItemReferencerUtils
import org.maurodata.domain.model.ItemUtils
import org.maurodata.util.DedupingObjectIdResolver

import com.fasterxml.jackson.annotation.JsonIdentityInfo
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.ObjectIdGenerators
import groovy.transform.AutoClone
import groovy.transform.CompileStatic
import groovy.transform.MapConstructor
import io.micronaut.core.annotation.Introspected
import io.micronaut.data.annotation.Index
import io.micronaut.data.annotation.Indexes
import io.micronaut.data.annotation.MappedEntity
import jakarta.persistence.Transient
import org.maurodata.domain.model.AdministeredItem
import org.maurodata.domain.model.ModelItem

/**
 * A TermRelationship defines a relation of a given type between two terms within a terminology.
 * <p>
 * A relationship belongs to a Terminology and defines a relation between two terms within that same terminology.
 * The type of the relationship is also defined within the same context - many relationships can exist of the same type.
 */
@CompileStatic
@AutoClone(excludes = ['terminology'])
@Introspected
@MappedEntity(schema = 'terminology')
@MapConstructor(includeSuperFields = true, includeSuperProperties = true, noArg = true)
@Indexes([
    @Index(columns = ['terminology_id']),
    @Index(columns = ['source_term_id']),
    @Index(columns = ['target_term_id']),
    @Index(columns = ['relationship_type_id'])])
class TermRelationship extends ModelItem<Terminology> implements ItemReferencer {

    @JsonIgnore
    Terminology terminology

    @JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator, property = 'code', scope = Term, resolver = DedupingObjectIdResolver)
    Term sourceTerm
    @JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator, property = 'code', scope = Term, resolver = DedupingObjectIdResolver)
    Term targetTerm

    @JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator, property = 'label', scope = TermRelationshipType, resolver = DedupingObjectIdResolver)
    TermRelationshipType relationshipType

    @Override
    String getLabel() {
        relationshipType.label
    }

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
        'tr'
    }

    @Override
    @Transient
    @JsonIgnore
    String getPathIdentifier() {
        "$sourceTerm.code.$label.$targetTerm.code"
    }

    /****
     * Methods for building a tree-like DSL
     */

    static TermRelationship build(
        Map args,
        @DelegatesTo(value = TermRelationship, strategy = Closure.DELEGATE_FIRST) Closure closure = {}) {
        new TermRelationship(args).tap(closure)
    }

    static TermRelationship build(
        @DelegatesTo(value = TermRelationship, strategy = Closure.DELEGATE_FIRST) Closure closure = {}) {
        build [:], closure
    }

    Term sourceTerm(Term sourceTerm) {
        this.sourceTerm = sourceTerm
    }

    Term sourceTerm(String sourceTermCode) {
        this.sourceTerm = terminology.terms.find {it.code == sourceTermCode}
    }

    Term targetTerm(Term targetTerm) {
        this.targetTerm = targetTerm
    }

    Term targetTerm(String targetTermCode) {
        this.targetTerm = terminology.terms.find {it.code == targetTermCode}
    }

    TermRelationshipType relationshipType(TermRelationshipType relationshipType) {
        this.relationshipType = relationshipType
    }

    TermRelationshipType relationshipType(String relationshipTypeLabel) {
        this.relationshipType = terminology.termRelationshipTypes.
            find {it.label == relationshipTypeLabel}
    }

    @Transient
    @JsonIgnore
    @Override
    List<ItemReference> retrieveItemReferences() {
        List<ItemReference> pathsBeingReferenced = [] + super.retrieveItemReferences()

        ItemReferencerUtils.addItem(sourceTerm, pathsBeingReferenced)
        ItemReferencerUtils.addItem(targetTerm, pathsBeingReferenced)
        ItemReferencerUtils.addItem(relationshipType, pathsBeingReferenced)

        return pathsBeingReferenced
    }

    @Transient
    @JsonIgnore
    @Override
    void replaceItemReferencesByIdentity(IdentityHashMap<Item, Item> replacements, Map<UUID, Item> allItemsById, List<Item> notReplaced) {
        super.replaceItemReferencesByIdentity(replacements, allItemsById, notReplaced)
        terminology = ItemReferencerUtils.replaceItemByIdentity(terminology, replacements, notReplaced)
        sourceTerm = ItemReferencerUtils.replaceItemByIdentity(sourceTerm, replacements, notReplaced)
        targetTerm = ItemReferencerUtils.replaceItemByIdentity(targetTerm, replacements, notReplaced)
        relationshipType = ItemReferencerUtils.replaceItemByIdentity(relationshipType, replacements, notReplaced)
    }

    @Override
    void copyInto(Item into) {
        super.copyInto(into)
        TermRelationship intoTermRelationship = (TermRelationship) into
        intoTermRelationship.terminology = ItemUtils.copyItem(this.terminology, intoTermRelationship.terminology)
        intoTermRelationship.sourceTerm = ItemUtils.copyItem(this.sourceTerm, intoTermRelationship.sourceTerm)
        intoTermRelationship.targetTerm = ItemUtils.copyItem(this.targetTerm, intoTermRelationship.targetTerm)
        intoTermRelationship.relationshipType = ItemUtils.copyItem(this.relationshipType, intoTermRelationship.relationshipType)
        // Depends on relationshipType
        intoTermRelationship.label = ItemUtils.copyItem(this.label, intoTermRelationship.label)
    }

    @Override
    Item shallowCopy() {
        TermRelationship termRelationshipShallowCopy = new TermRelationship()
        this.copyInto(termRelationshipShallowCopy)
        return termRelationshipShallowCopy
    }
}
