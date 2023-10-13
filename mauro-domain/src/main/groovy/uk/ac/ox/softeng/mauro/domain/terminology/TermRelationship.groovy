
package uk.ac.ox.softeng.mauro.domain.terminology

import com.fasterxml.jackson.annotation.JsonIgnore
import groovy.transform.CompileStatic
import groovy.transform.MapConstructor
import io.micronaut.core.annotation.Introspected
import io.micronaut.data.annotation.Index
import io.micronaut.data.annotation.Indexes
import io.micronaut.data.annotation.MappedEntity
import jakarta.persistence.Transient
import uk.ac.ox.softeng.mauro.domain.model.ModelItem

@CompileStatic
@Introspected
@MappedEntity
@MapConstructor(includeSuperFields = true, includeSuperProperties = true)
@Indexes([@Index(columns = ['terminology_id']), @Index(columns = ['source_term_id']), @Index(columns = ['target_term_id']), @Index(columns = ['relationship_type_id'])])
class TermRelationship extends ModelItem<Terminology> {

    @Transient
    String domainType = TermRelationship.simpleName

    @JsonIgnore
    Terminology terminology

    Term sourceTerm

    Term targetTerm

    TermRelationshipType relationshipType

    @Override
    @Transient
    @JsonIgnore
    Terminology getParent() {
        terminology
    }

    static TermRelationship build(Map args, @DelegatesTo(value = TermRelationship.class, strategy = Closure.DELEGATE_FIRST) Closure closure = {}) {
        new TermRelationship(args).tap(closure)
    }

    static TermRelationship build(@DelegatesTo(value = TermRelationship.class, strategy = Closure.DELEGATE_FIRST) Closure closure = {}) {
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
        this.relationshipType = terminology.termRelationshipTypes.find {it.label == relationshipTypeLabel}
    }



}
