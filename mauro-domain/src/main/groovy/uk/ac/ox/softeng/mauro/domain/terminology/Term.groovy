package uk.ac.ox.softeng.mauro.domain.terminology

import com.fasterxml.jackson.annotation.JsonBackReference
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonManagedReference
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
import uk.ac.ox.softeng.mauro.domain.model.AdministeredItem
import uk.ac.ox.softeng.mauro.domain.model.ModelItem

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
class Term extends ModelItem<Terminology> {

    @Override
    String getLabel() {
        code && definition && code == definition ? code :
        code && definition ? "${code}: ${definition}" :
        code
    }

    @JsonIgnore
    Terminology terminology

    @NotBlank
    @Pattern(regexp = /[^\$@|]*/, message = 'Cannot contain $, | or @')
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

    @Transient
    @JsonIgnore
    Set<CodeSet> getCodeSets() {
        codeSets
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
        'tm'
    }

    @Override
    @Transient
    @JsonIgnore
    String getPathIdentifier() {
        code
    }

    /****
     * Methods for building a tree-like DSL
     */
    static Term build(
            Map args,
            @DelegatesTo(value = Term, strategy = Closure.DELEGATE_FIRST) Closure closure = { }) {
        new Term(args).tap(closure)
    }

    static Term build(@DelegatesTo(value = Term, strategy = Closure.DELEGATE_FIRST) Closure closure = { }) {
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
}
