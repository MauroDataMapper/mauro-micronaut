package org.maurodata.persistence.classifier

import groovy.transform.CompileStatic
import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.maurodata.domain.classifier.Classifier
import org.maurodata.persistence.cache.AdministeredItemCacheableRepository
import org.maurodata.persistence.model.AdministeredItemContentRepository

@CompileStatic
@Singleton
class ClassifierContentRepository extends AdministeredItemContentRepository {

    @Inject
    AdministeredItemCacheableRepository.ClassifierCacheableRepository classifierCacheableRepository

    @Override
    Classifier readWithContentById(UUID id) {
        Classifier classifier = classifierCacheableRepository.readById(id)
        if (!classifier.parentClassifier) {
            classifier.childClassifiers = classifierCacheableRepository.readAllByParentClassifier_Id(classifier.id)
        }
        classifier
    }

}
