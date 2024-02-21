package uk.ac.ox.softeng.mauro.persistence.terminology

import groovy.transform.CompileStatic
import io.micronaut.core.annotation.Nullable
import io.micronaut.data.jdbc.annotation.JdbcRepository
import io.micronaut.data.model.query.builder.sql.Dialect
import jakarta.inject.Inject
import uk.ac.ox.softeng.mauro.domain.model.AdministeredItem
import uk.ac.ox.softeng.mauro.domain.terminology.TermRelationship
import uk.ac.ox.softeng.mauro.domain.terminology.Terminology
import uk.ac.ox.softeng.mauro.persistence.model.ModelItemRepository
import uk.ac.ox.softeng.mauro.persistence.terminology.dto.TermRelationshipDTORepository

@CompileStatic
@JdbcRepository(dialect = Dialect.POSTGRES)
abstract class TermRelationshipRepository implements ModelItemRepository<TermRelationship> {

    @Inject
    TermRelationshipDTORepository termRelationshipDTORepository

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

    @Nullable
    abstract List<TermRelationship> readAllByTerminology(Terminology terminology)

    @Override
    @Nullable
    List<TermRelationship> readAllByParent(AdministeredItem parent) {
        readAllByTerminology((Terminology) parent)
    }

    @Nullable
    abstract Long deleteByTerminologyId(UUID terminologyId)

//    @Override
    Long deleteByOwnerId(UUID ownerId) {
        deleteByTerminologyId(ownerId)
    }

    @Override
    Class getDomainClass() {
        TermRelationship
    }
}
