package org.maurodata.persistence.terminology

import groovy.transform.CompileStatic
import io.micronaut.core.annotation.Nullable
import io.micronaut.data.annotation.Query
import io.micronaut.data.annotation.Repository
import io.micronaut.data.jdbc.annotation.JdbcRepository
import io.micronaut.data.model.query.builder.sql.Dialect
import jakarta.inject.Inject
import org.maurodata.FieldConstants
import org.maurodata.domain.terminology.Terminology
import org.maurodata.persistence.ContentsService
import org.maurodata.persistence.dto.HasChildrenDTO
import org.maurodata.persistence.model.ModelRepository
import org.maurodata.persistence.terminology.dto.TerminologyDTORepository

@CompileStatic
@Repository
@JdbcRepository(dialect = Dialect.POSTGRES)
abstract class TerminologyRepository implements ModelRepository<Terminology> {

    @Inject
    TerminologyDTORepository terminologyDTORepository

    TerminologyRepository(ContentsService contentsService) {
        this.contentsService = contentsService
    }


    @Nullable
    Terminology findById(UUID id) {
        terminologyDTORepository.findById(id)
    }

    @Nullable
    List<Terminology> findAllByParentAndPathIdentifier(UUID item, String pathIdentifier) {
        terminologyDTORepository.findAllByParentAndPathIdentifier(item, pathIdentifier)
    }

    @Nullable
    @Override
    List<Terminology> findAllByLabel(String label){
        terminologyDTORepository.findAllByLabel(label)
    }

    @Override
    Class getDomainClass() {
        Terminology
    }

    @Override
    Boolean handles(Class clazz) {
        domainClass.isAssignableFrom(clazz)
    }

    @Override
    @Nullable
    abstract List<Terminology> readAllByFolderIdIn(Collection<UUID> folderIds)

    @Override
    @Nullable
    abstract List<Terminology> findAllByFolderId(UUID folderId)

    @Query(value = '''
        select id,
        exists(select 1 from terminology.term where terminology_id = t.id) as has_children
        from terminology.terminology as t
        where t.id in (:tids)''', nativeQuery = true)
    abstract List<HasChildrenDTO> getHasChildrenDTOs(Collection<UUID> tids)



    @Override
    Boolean handles(String domainType) {
        return domainType != null && domainType.toLowerCase() in [FieldConstants.TERMINOLOGY_LOWERCASE, FieldConstants.TERMINOLOGIES_LOWERCASE]
    }

    Boolean handlesPathPrefix(final String pathPrefix) {
        'te'.equalsIgnoreCase(pathPrefix)
    }
}
