package uk.ac.ox.softeng.mauro.persistence.terminology

import groovy.transform.CompileStatic
import io.micronaut.data.jdbc.annotation.JdbcRepository
import io.micronaut.data.model.query.builder.sql.Dialect
import jakarta.inject.Inject
import uk.ac.ox.softeng.mauro.domain.model.AdministeredItem
import uk.ac.ox.softeng.mauro.domain.terminology.TermRelationshipType
import uk.ac.ox.softeng.mauro.domain.terminology.Terminology
import uk.ac.ox.softeng.mauro.persistence.model.ModelItemRepository
import uk.ac.ox.softeng.mauro.persistence.terminology.dto.TermRelationshipTypeDTORepository

@CompileStatic
@JdbcRepository(dialect = Dialect.POSTGRES)
abstract class TermRelationshipTypeRepository implements ModelItemRepository<TermRelationshipType> {

    @Inject
    TermRelationshipTypeDTORepository termRelationshipTypeDTORepository

    TermRelationshipType findById(UUID id) {
        termRelationshipTypeDTORepository.findById(id) as TermRelationshipType
    }

    List<TermRelationshipType> findAllByTerminology(Terminology terminology) {
        termRelationshipTypeDTORepository.findAllByTerminology(terminology) as List<TermRelationshipType>
    }

    @Override
    List<TermRelationshipType> findAllByParent(AdministeredItem parent) {
        findAllByTerminology((Terminology) parent)
    }

    abstract List<TermRelationshipType> readAllByTerminology(Terminology terminology)

    @Override
    List<TermRelationshipType> readAllByParent(AdministeredItem parent) {
        readAllByTerminology((Terminology) parent)
    }

    abstract Long deleteByTerminologyId(UUID terminologyId)

//    @Override
    Long deleteByOwnerId(UUID ownerId) {
        deleteByTerminologyId(ownerId)
    }

    @Override
    Class getDomainClass() {
        TermRelationshipType
    }
}
