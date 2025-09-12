package org.maurodata.persistence.terminology

import groovy.transform.CompileStatic
import io.micronaut.core.annotation.Nullable
import io.micronaut.data.annotation.Repository
import io.micronaut.data.jdbc.annotation.JdbcRepository
import io.micronaut.data.model.query.builder.sql.Dialect
import jakarta.inject.Inject
import org.maurodata.FieldConstants
import org.maurodata.domain.terminology.Terminology
import org.maurodata.persistence.model.ModelRepository
import org.maurodata.persistence.terminology.dto.TerminologyDTORepository

@CompileStatic
@Repository
@JdbcRepository(dialect = Dialect.POSTGRES)
abstract class TerminologyRepository implements ModelRepository<Terminology> {

    @Inject
    TerminologyDTORepository terminologyDTORepository

    @Nullable
    Terminology findById(UUID id) {
        terminologyDTORepository.findById(id)
    }

    @Nullable
    List<Terminology> findAllByParentAndPathIdentifier(UUID item, String pathIdentifier) {
        terminologyDTORepository.findAllByParentAndPathIdentifier(item, pathIdentifier)
    }

    Terminology findByPathIdentifier(String pathIdentifier){
        terminologyDTORepository.findByLabel(pathIdentifier)
    }
    @Override
    Class getDomainClass() {
        Terminology
    }

    @Override
    @Nullable
    abstract List<Terminology> findAllByFolderId(UUID folderId)

    @Override
    Boolean handles(String domainType) {
        return domainType != null && domainType.toLowerCase() in [FieldConstants.TERMINOLOGY_LOWERCASE, FieldConstants.TERMINOLOGIES_LOWERCASE]
    }

    Boolean handlesPathPrefix(final String pathPrefix) {
        'te'.equalsIgnoreCase(pathPrefix)
    }
}
