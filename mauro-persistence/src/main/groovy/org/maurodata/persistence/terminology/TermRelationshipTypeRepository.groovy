package org.maurodata.persistence.terminology

import groovy.transform.CompileStatic
import io.micronaut.core.annotation.NonNull
import io.micronaut.core.annotation.Nullable
import io.micronaut.data.jdbc.annotation.JdbcRepository
import io.micronaut.data.model.query.builder.sql.Dialect
import jakarta.inject.Inject
import org.maurodata.domain.model.AdministeredItem
import org.maurodata.domain.terminology.TermRelationshipType
import org.maurodata.domain.terminology.Terminology
import org.maurodata.persistence.model.ModelItemRepository
import org.maurodata.persistence.terminology.dto.TermRelationshipTypeDTORepository

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
    List<TermRelationshipType> findAllByParentAndPathIdentifier(UUID item, String pathIdentifier) {
        termRelationshipTypeDTORepository.findAllByParentAndPathIdentifier(item, pathIdentifier)
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
    @Override
    TermRelationshipType findByLabelContaining(String pathIdentifier){
        termRelationshipTypeDTORepository.findByLabel(pathIdentifier)
    }
    @Nullable
    abstract Long deleteByTerminologyId(UUID terminologyId)


//    @Override
    Long deleteByOwnerId(UUID ownerId) {
        deleteByTerminologyId(ownerId)
    }

    @Override
    Class getDomainClass() {
        TermRelationshipType
    }

    Boolean handlesPathPrefix(final String pathPrefix) {
        'trt'.equalsIgnoreCase(pathPrefix)
    }
}
