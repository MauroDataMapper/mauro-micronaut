
package uk.ac.ox.softeng.mauro.domain.terminology

import com.fasterxml.jackson.annotation.JsonIgnore
import groovy.transform.CompileStatic
import io.micronaut.core.annotation.Introspected
import io.micronaut.data.annotation.Index
import io.micronaut.data.annotation.Indexes
import io.micronaut.data.annotation.MappedEntity
import jakarta.persistence.Transient
import uk.ac.ox.softeng.mauro.domain.model.ModelItem

@CompileStatic
@Introspected
@MappedEntity
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
}
