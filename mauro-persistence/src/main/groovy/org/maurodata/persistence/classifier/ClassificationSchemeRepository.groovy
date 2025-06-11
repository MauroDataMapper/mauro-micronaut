package org.maurodata.persistence.classifier

import groovy.transform.CompileStatic
import io.micronaut.core.annotation.Nullable
import io.micronaut.data.jdbc.annotation.JdbcRepository
import io.micronaut.data.model.query.builder.sql.Dialect
import jakarta.inject.Inject
import org.maurodata.domain.classifier.ClassificationScheme
import org.maurodata.persistence.classifier.dto.ClassificationSchemeDTORepository
import org.maurodata.persistence.model.ModelRepository

@CompileStatic
@JdbcRepository(dialect = Dialect.POSTGRES)
abstract class ClassificationSchemeRepository implements ModelRepository<ClassificationScheme> {

    @Inject
    ClassificationSchemeDTORepository classificationSchemeDTORepository

    @Nullable
    ClassificationScheme findById(UUID id) {
        classificationSchemeDTORepository.findById(id) as ClassificationScheme
    }

    @Override
    Class getDomainClass() {
        ClassificationScheme
    }

    @Override
    @Nullable
    abstract List<ClassificationScheme> findAllByFolderId(UUID folderId)

}
