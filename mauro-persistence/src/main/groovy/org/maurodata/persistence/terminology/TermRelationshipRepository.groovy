package org.maurodata.persistence.terminology

import groovy.transform.CompileStatic
import io.micronaut.core.annotation.Nullable
import io.micronaut.data.annotation.Join
import io.micronaut.data.jdbc.annotation.JdbcRepository
import io.micronaut.data.model.query.builder.sql.Dialect
import jakarta.inject.Inject
import org.maurodata.domain.model.AdministeredItem
import org.maurodata.domain.terminology.Term
import org.maurodata.domain.terminology.TermRelationship
import org.maurodata.domain.terminology.TermRelationshipType
import org.maurodata.domain.terminology.Terminology
import org.maurodata.persistence.model.ModelItemRepository
import org.maurodata.persistence.terminology.dto.TermRelationshipDTORepository

@CompileStatic
@JdbcRepository(dialect = Dialect.POSTGRES)
abstract class TermRelationshipRepository implements ModelItemRepository<TermRelationship> {

    @Inject
    TermRelationshipDTORepository termRelationshipDTORepository

    @Override
    @Join(value = 'sourceTerm', type = Join.Type.LEFT_FETCH)
    @Join(value = 'targetTerm', type = Join.Type.LEFT_FETCH)
    @Join(value = 'relationshipType', type = Join.Type.LEFT_FETCH)
    @Nullable
    abstract TermRelationship readById(UUID id)

    @Override
    @Nullable
    TermRelationship findById(UUID id) {
        termRelationshipDTORepository.findById(id) as TermRelationship
    }

    @Nullable
    List<TermRelationship> findAllByTerminology(Terminology terminology) {
        termRelationshipDTORepository.findAllByTerminology(terminology) as List<TermRelationship>
    }

    @Override
    @Nullable
    List<TermRelationship> findAllByParent(AdministeredItem parent) {
        findAllByTerminology((Terminology) parent)
    }

    @Join(value = 'sourceTerm', type = Join.Type.LEFT_FETCH)
    @Join(value = 'targetTerm', type = Join.Type.LEFT_FETCH)
    @Join(value = 'relationshipType', type = Join.Type.LEFT_FETCH)
    @Nullable
    abstract List<TermRelationship> readAllByTerminology(Terminology terminology)

    @Override
    @Nullable
    List<TermRelationship> readAllByParent(AdministeredItem parent) {
        readAllByTerminology((Terminology) parent)
    }

    @Nullable
    abstract List<TermRelationship> readAllBySourceTerm(Term sourceTerm)

    @Nullable
    abstract List<TermRelationship> readAllByTargetTerm(Term targetTerm)

    @Nullable
    abstract List<TermRelationship> readAllByRelationshipType(TermRelationshipType relationshipType)

    @Nullable
    List<TermRelationship> readAllByTerminologyAndSourceTermOrTargetTerm(Terminology terminology, Term term) {
        termRelationshipDTORepository.findAllByTerminologyAndSourceTermOrTargetTerm(terminology, term, term) as List<TermRelationship>
    }

    @Override
    Class getDomainClass() {
        TermRelationship
    }
}
