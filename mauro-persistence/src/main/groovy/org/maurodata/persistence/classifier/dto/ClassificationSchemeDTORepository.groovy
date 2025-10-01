package org.maurodata.persistence.classifier.dto

import groovy.transform.CompileStatic
import io.micronaut.core.annotation.Nullable
import io.micronaut.data.annotation.Join
import io.micronaut.data.annotation.Query
import io.micronaut.data.jdbc.annotation.JdbcRepository
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.repository.GenericRepository
import org.maurodata.domain.classifier.ClassificationScheme

@CompileStatic
@JdbcRepository(dialect = Dialect.POSTGRES)
abstract class ClassificationSchemeDTORepository implements GenericRepository<ClassificationSchemeDTO, UUID> {

    @Join(value = 'authority', type = Join.Type.LEFT_FETCH)
    @Join(value = 'catalogueUser', type = Join.Type.LEFT_FETCH)
    @Nullable
    abstract ClassificationSchemeDTO findById(UUID id)

    @Nullable
    @Query('select * from core.classification_scheme where folder_id = :item AND label = :pathIdentifier')
    abstract List<ClassificationSchemeDTO> findAllByParentAndPathIdentifier(UUID item, String pathIdentifier)

    @Nullable
    @Query('select * from core.classification_scheme where label like :label')
    abstract List<ClassificationScheme> findAllByLabelContaining(String label)

}
