package org.maurodata.persistence.classifier

import groovy.transform.CompileStatic
import io.micronaut.core.annotation.NonNull
import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.maurodata.domain.classifier.ClassificationScheme
import org.maurodata.domain.model.AdministeredItem
import org.maurodata.persistence.cache.AdministeredItemCacheableRepository.ClassifierCacheableRepository
import org.maurodata.persistence.cache.ModelCacheableRepository
import org.maurodata.persistence.model.ModelContentRepository

@CompileStatic
@Singleton
class ClassificationSchemeContentRepository extends ModelContentRepository<ClassificationScheme> {

    @Inject
    ModelCacheableRepository.ClassificationSchemeCacheableRepository classificationSchemeCacheableRepository
    @Inject
    ClassifierCacheableRepository classifierCacheableRepository

    @Override
    ClassificationScheme findWithContentById(UUID id) {
        ClassificationScheme classificationScheme = classificationSchemeCacheableRepository.findById(id)
        classificationScheme.csClassifiers = classifierCacheableRepository.findAllByParent(classificationScheme)
        classificationScheme
    }

    @Override
    ClassificationScheme saveWithContent(@NonNull ClassificationScheme model) {
        ClassificationScheme saved = (ClassificationScheme) super.saveWithContent(model)
        classifierRepository.updateAll(saved.csClassifiers.findAll{ it.classificationScheme})
        saved
    }

    // TODO overridden code not invalidating cache
    @Override
    Long deleteWithContent(@NonNull AdministeredItem administeredItem) {
        List<Collection<AdministeredItem>> associations = administeredItem.getAllAssociations()

        // delete the association contents in reverse order
        associations.reverse().each {association ->
            if (association) {
                deleteAllJoinAdministeredItemToClassifier(association)
                deleteAllFacets(association)
                getRepository(association.first()).deleteAll(association)
            }
        }
        deleteAllFacets(administeredItem)
        administeredItem.classifiers.each{
            classifierRepository.deleteAllJoinAdministeredItemToClassifier(it)
        }
        classifierRepository.deleteAll(administeredItem.classifiers)
        getRepository(administeredItem).delete(administeredItem)
    }

    @Override
    Boolean handles(String domainType) {
        return classificationSchemeCacheableRepository.handles(domainType)
    }

    @Override
    Boolean handles(Class clazz) {
        return classificationSchemeCacheableRepository.handles(clazz)
    }
}
