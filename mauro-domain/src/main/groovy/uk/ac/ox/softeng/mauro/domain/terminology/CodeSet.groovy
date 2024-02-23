package uk.ac.ox.softeng.mauro.domain.terminology

import com.fasterxml.jackson.annotation.JsonIdentityInfo
import com.fasterxml.jackson.annotation.JsonIgnore
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
import jakarta.persistence.JoinColumn
import jakarta.persistence.JoinTable
import jakarta.persistence.ManyToMany
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

   // @Relation(value = Relation.Kind.MANY_TO_MANY, cascade = Relation.Cascade.ALL)
    @ManyToMany
    @JoinTable (
            name = "codeSet_term",
            joinColumns = @JoinColumn( name = "id"),
            inverseJoinColumns = @JoinColumn (name = "id"))
    List<Term> terms = []

    @Override
    @Transient
    @JsonIgnore
    List<List<? extends ModelItem<CodeSet>>> getAllAssociations() {
        [terms] as List<List<? extends ModelItem<CodeSet>>>
    }

    @Transient
    @JsonIgnore
    CodeSet setAssociations() {
        Map<UUID, Term> termsMap = terms.collectEntries {[it.id, it]}
        terms.each {
            it.parent = this
        }
        this
    }
    @Override
    @Transient
    @JsonIgnore
    String getPathPrefix() {
        'co'
    }

    @Override
    CodeSet clone() {
        log.debug '*** CodeSet clone() ***'
        CodeSet cloned = (CodeSet) super.clone()

    }


}
