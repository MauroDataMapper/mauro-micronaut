package uk.ac.ox.softeng.mauro.persistence.terminology

import groovy.transform.CompileStatic
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
    TermRelationship findById(UUID id) {
        termRelationshipDTORepository.findById(id) as TermRelationship
    }

    List<TermRelationship> findAllByTerminology(Terminology terminology) {
        termRelationshipDTORepository.findAllByTerminology(terminology) as List<TermRelationship>
    }

    @Override
    List<TermRelationship> findAllByParent(AdministeredItem parent) {
        findAllByTerminology((Terminology) parent)
    }

    abstract List<TermRelationship> readAllByTerminology(Terminology terminology)

    @Override
    List<TermRelationship> readAllByParent(AdministeredItem parent) {
        readAllByTerminology((Terminology) parent)
    }

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
