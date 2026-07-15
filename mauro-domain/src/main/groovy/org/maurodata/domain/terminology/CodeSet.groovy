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

import com.fasterxml.jackson.annotation.JsonIgnore
import groovy.transform.AutoClone
import groovy.transform.CompileStatic
import groovy.transform.MapConstructor
import groovy.util.logging.Slf4j
import io.micronaut.core.annotation.Introspected
import io.micronaut.data.annotation.Index
import io.micronaut.data.annotation.Indexes
import io.micronaut.data.annotation.MappedEntity
import io.micronaut.data.annotation.Relation
import jakarta.persistence.JoinColumn
import jakarta.persistence.JoinTable
import jakarta.persistence.Transient
import org.maurodata.domain.model.Model
import org.maurodata.visitor.DomainVisitor

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
class CodeSet extends Model implements ItemReferencer, DiffableItem<CodeSet> {

    @Relation(value = Relation.Kind.MANY_TO_MANY, cascade = Relation.Cascade.ALL)
    @JoinTable(
        name = 'code_set_term',
        joinColumns = @JoinColumn(name = 'code_set_id'),
        inverseJoinColumns = @JoinColumn(name = 'term_id')
    )
    Set<Term> terms = []

    // This attribute is used when creating a new CodeSet and wanting to add all terms from one or more terminologies.
    @Transient
    Set<Terminology> terminologies = []

    @Override
    <T> T accept(DomainVisitor<T> visitor) {
        return visitor.visitCodeSet(this)
    }


    @Override
    @Transient
    @JsonIgnore
    String getPathPrefix() {
        'cs'
    }

    @Override
    CodeSet clone() {
        CodeSet cloned = (CodeSet) super.clone()
        cloned.setAssociations()
        cloned
    }

    CodeSet addTerm(Term term) {
        terms.add(term)
        this
    }


    @Override
    String toString() {
        return "CodeSet{" +
               "terms=" + terms +
               '}'
    }

    @PrePersist
    @PreUpdate
    void prePersist() {
        super.prePersist()
        if (!getModelWithVersion()) {
            branchName = 'main'
        }
    }

    int hashCode() {
        return (terms != null ? terms.hashCode() : 0)
    }

    @Transient
    @JsonIgnore
    @Override
    List<ItemReference> retrieveItemReferences() {
        List<ItemReference> pathsBeingReferenced = [] + super.retrieveItemReferences()

        ItemReferencerUtils.addItems(terms, pathsBeingReferenced)

        return pathsBeingReferenced
    }

    @Override
    void replaceItemReferencesByIdentity(IdentityHashMap<Item, Item> replacements, Map<UUID, Item> allItemsById, List<Item> notReplaced) {
        super.replaceItemReferencesByIdentity(replacements, allItemsById, notReplaced)

        terms = ItemReferencerUtils.replaceItemsByIdentity(terms, replacements, notReplaced)
    }

    @Override
    void copyInto(Item into) {
        super.copyInto(into)
        CodeSet intoCodeSet = (CodeSet) into
        intoCodeSet.terms = ItemUtils.copyItems(this.terms, intoCodeSet.terms)
    }

    @Override
    Item shallowCopy() {
        CodeSet codeSetShallowCopy = new CodeSet()
        this.copyInto(codeSetShallowCopy)
        return codeSetShallowCopy
    }

    @Override
    @JsonIgnore
    @Transient
    ObjectDiff<CodeSet> diff(CodeSet other, String lhsPathRoot, String rhsPathRoot) {
        ObjectDiff<CodeSet> base = DiffBuilder.objectDiff(CodeSet)
            .leftHandSide(id?.toString(), this)
            .rightHandSide(other.id?.toString(), other)
        base.label = this.label
        base.appendString(DiffBuilder.DESCRIPTION, this.description, other.description, this, other)
        base.appendString(DiffBuilder.ALIASES_STRING, this.aliasesString, other.aliasesString, this, other)
        if (!DiffBuilder.isNullOrEmpty(this.terms as Collection<Object>) || !DiffBuilder.isNullOrEmpty(other.terms as Collection<Object>)) {
            base.appendCollection(DiffBuilder.TERMS, this.terms as Collection<DiffableItem>, other.terms as Collection<DiffableItem>, lhsPathRoot,
                                  rhsPathRoot)
        }
        base
    }

    /**
     * Builder methods
     * @param args
     * @param closure
     * @return
     */
    static CodeSet build(
        Map args,
        @DelegatesTo(value = CodeSet, strategy = Closure.DELEGATE_FIRST) Closure closure = {}) {
        new CodeSet(args).tap(closure)
    }

    static CodeSet build(
        @DelegatesTo(value = CodeSet, strategy = Closure.DELEGATE_FIRST) Closure closure = {}) {
        build [:], closure
    }


    Term term (Term term) {
        this.terms.add(term)
        term
    }

    Term term(Map args, @DelegatesTo(value = Term, strategy = Closure.DELEGATE_FIRST) Closure closure = {}) {
        Term term1 = Term.build(args , closure)
        term term1
    }

    Term term(@DelegatesTo(value = Term, strategy = Closure.DELEGATE_FIRST) Closure closure = {}) {
        term [:], closure
    }

}
