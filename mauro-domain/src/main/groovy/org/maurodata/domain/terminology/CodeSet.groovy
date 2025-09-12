package org.maurodata.domain.terminology

import org.maurodata.domain.model.ItemReference
import org.maurodata.domain.model.ItemReferencer

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
class CodeSet extends Model implements ItemReferencer {

    @Relation(value = Relation.Kind.MANY_TO_MANY, cascade = Relation.Cascade.ALL)
    @JoinTable(
            name = 'code_set_term',
            joinColumns = @JoinColumn(name = 'code_set_id'),
            inverseJoinColumns = @JoinColumn(name = 'term_id')
    )
    @JsonIgnore
    Set<Term> terms = []

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


    @Transient
    @JsonIgnore
    @Override
    void setAssociations() {
        []
    }


    @Override
    String toString() {
        return "CodeSet{" +
                "terms=" + terms +
                '}'
    }

    /*
    boolean equals(o) {
        if (this.is(o)) return true
        if (!(o instanceof CodeSet)) return false

        CodeSet codeSet = (CodeSet) o

        if (terms != codeSet.terms) return false

        return true
    }
    */

    int hashCode() {
        return (terms != null ? terms.hashCode() : 0)
    }

    @Transient
    @JsonIgnore
    @Override
    List<ItemReference> getItemReferences() {
        List<ItemReference> pathsBeingReferenced = []
        if (terms != null) {
            terms.forEach {Term term ->
                pathsBeingReferenced << ItemReference.from(term)
            }
        }
        return pathsBeingReferenced
    }

    @Override
    void replaceItemReferences(Map<UUID, ItemReference> replacements) {
        if (terms != null) {
            final Set<Term> replacementSet = []
            terms.forEach {Term term ->
                ItemReference replacementItemReference = replacements.get(term.id)
                if (replacementItemReference != null) {
                    replacementSet.add((Term) replacementItemReference.theItem)
                } else {
                    replacementSet.add(term)
                }
            }
            terms = replacementSet
        }
    }
}
