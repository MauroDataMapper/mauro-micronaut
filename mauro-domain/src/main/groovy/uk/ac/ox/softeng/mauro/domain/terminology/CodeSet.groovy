package uk.ac.ox.softeng.mauro.domain.terminology

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
import uk.ac.ox.softeng.mauro.domain.model.Model
import uk.ac.ox.softeng.mauro.domain.model.ModelItem

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
class CodeSet extends Model {

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
    }

    CodeSet addTerm(Term term) {
        terms.add(term)
        this
    }

    @Transient
    @JsonIgnore
    List<List<ModelItem<CodeSet>>> getAllAssociations() {
        [terms] as List<List<ModelItem<CodeSet>>>
    }

    @Transient
    @JsonIgnore
    @Override
    void setAssociations() {
//        terms.each {
//            it.codeSets.add(this)
//        }
       // todo: Don't do the above, causes: 1)stackoverflow error on codeSet.toString();  2)term.toString() -cannot evaluate
        this
    }


    @Override
    String toString() {
        return "CodeSet{" +
                "terms=" + terms +
                '}';
    }

    boolean equals(o) {
        if (this.is(o)) return true
        if (!(o instanceof CodeSet)) return false

        CodeSet codeSet = (CodeSet) o

        if (terms != codeSet.terms) return false

        return true
    }

    int hashCode() {
        return (terms != null ? terms.hashCode() : 0)
    }
}
