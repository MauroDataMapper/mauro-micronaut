package uk.ac.ox.softeng.mauro.persistence.classifier

import groovy.transform.CompileStatic
import jakarta.inject.Inject
import jakarta.inject.Singleton
import uk.ac.ox.softeng.mauro.domain.classifier.Classifier
import uk.ac.ox.softeng.mauro.persistence.model.AdministeredItemContentRepository

@CompileStatic
@Singleton
class ClassifierContentRepository extends AdministeredItemContentRepository {

    @Inject
    ClassifierRepository classifierRepository

  //  @Override
//    Classifier findWithContentById(UUID id) {
//        Classifier classifier = classifierRepository.findById(id)
//        classifier.childClassifiers = classifierRepository.findallByParent(classifier)
//        classifier
//    }

//    @Override
//    ClassificationScheme saveWithContent(@NonNull ClassificationScheme model) {
//        ClassificationScheme saved = (ClassificationScheme) super.saveWithContent(model)
//        classifierRepository.updateAll(saved.classifiers.findAll{ it.classificationScheme})
//        saved
//    }
}
