package uk.ac.ox.softeng.mauro.persistence.terminology

import groovy.transform.CompileStatic
import io.micronaut.core.annotation.NonNull
import io.micronaut.core.annotation.Nullable
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

    @Nullable
    TermRelationshipType findById(UUID id) {
        termRelationshipTypeDTORepository.findById(id) as TermRelationshipType
    }

    @Nullable
    List<TermRelationshipType> findAllByTerminology(Terminology terminology) {
        termRelationshipTypeDTORepository.findAllByTerminology(terminology) as List<TermRelationshipType>
    }

    @Override
    @Nullable
    List<TermRelationshipType> findAllByParent(AdministeredItem parent) {
        findAllByTerminology((Terminology) parent)
    }

    @Nullable
    abstract List<TermRelationshipType> readAllByTerminology(Terminology terminology)

    @Override
    @Nullable
    List<TermRelationshipType> readAllByParent(AdministeredItem parent) {
        readAllByTerminology((Terminology) parent)
    }

    @Nullable
    abstract Long deleteByTerminologyId(UUID terminologyId)

    @Override
    @Nullable
    TermRelationshipType findWithContentById(@NonNull UUID id, @NonNull AdministeredItem parent){
        TermRelationshipType termRelationshipType = findById(id)
        termRelationshipType.parent = parent
        termRelationshipType
    }
//    @Override
    Long deleteByOwnerId(UUID ownerId) {
        deleteByTerminologyId(ownerId)
    }

    @Override
    Class getDomainClass() {
        TermRelationshipType
    }
}
