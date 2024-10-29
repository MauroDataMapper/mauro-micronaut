package uk.ac.ox.softeng.mauro.persistence.classifier

import groovy.transform.CompileStatic
import jakarta.inject.Inject
import jakarta.inject.Singleton
import uk.ac.ox.softeng.mauro.domain.classifier.Classifier
import uk.ac.ox.softeng.mauro.persistence.cache.AdministeredItemCacheableRepository
import uk.ac.ox.softeng.mauro.persistence.model.AdministeredItemContentRepository

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
