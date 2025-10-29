package org.maurodata.persistence.classifier

import groovy.transform.CompileStatic
import io.micronaut.core.annotation.Nullable
import io.micronaut.data.jdbc.annotation.JdbcRepository
import io.micronaut.data.model.query.builder.sql.Dialect
import jakarta.inject.Inject
import org.maurodata.FieldConstants
import org.maurodata.domain.classifier.ClassificationScheme
import org.maurodata.persistence.ContentsService
import org.maurodata.persistence.classifier.dto.ClassificationSchemeDTORepository
import org.maurodata.persistence.model.ModelRepository

@CompileStatic
@JdbcRepository(dialect = Dialect.POSTGRES)
abstract class ClassificationSchemeRepository implements ModelRepository<ClassificationScheme> {

    ClassificationSchemeRepository(ContentsService contentsService) {
        this.contentsService = contentsService
    }

    @Inject
    ClassificationSchemeDTORepository classificationSchemeDTORepository

    @Nullable
    ClassificationScheme findById(UUID id) {
        classificationSchemeDTORepository.findById(id) as ClassificationScheme
    }

    @Nullable
    List<ClassificationScheme> findAllByParentAndPathIdentifier(UUID item, String pathIdentifier) {
        classificationSchemeDTORepository.findAllByParentAndPathIdentifier(item,pathIdentifier) as List<ClassificationScheme>
    }

    List<ClassificationScheme> findAllByLabel(String label) {
        classificationSchemeDTORepository.findAllByLabel(label)
    }

    @Override
    Class getDomainClass() {
        ClassificationScheme
    }

    @Override
    Boolean handles(Class clazz) {
        domainClass.isAssignableFrom(clazz)
    }

    @Override
    @Nullable
    abstract List<ClassificationScheme> findAllByFolderId(UUID folderId)

    @Override
    @Nullable
    abstract List<ClassificationScheme> readAllByFolderIdIn(Collection<UUID> folderIds)


    @Override
    Boolean handles(String domainType){
        return domainType != null && domainType.toLowerCase() in [FieldConstants.CLASSIFICATION_SCHEME_LOWERCASE, FieldConstants.CLASSIFICATION_SCHEMES_LOWERCASE ]
    }
    Boolean handlesPathPrefix(final String pathPrefix) {
        'csc'.equalsIgnoreCase(pathPrefix)
    }

}
