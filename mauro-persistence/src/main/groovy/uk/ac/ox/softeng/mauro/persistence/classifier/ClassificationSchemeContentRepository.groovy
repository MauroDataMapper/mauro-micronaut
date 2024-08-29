package uk.ac.ox.softeng.mauro.persistence.classifier

import groovy.transform.CompileStatic
import io.micronaut.core.annotation.NonNull
import jakarta.inject.Inject
import jakarta.inject.Singleton
import uk.ac.ox.softeng.mauro.domain.classifier.ClassificationScheme
import uk.ac.ox.softeng.mauro.domain.model.AdministeredItem
import uk.ac.ox.softeng.mauro.persistence.model.ModelContentRepository

@CompileStatic
@Singleton
class ClassificationSchemeContentRepository extends ModelContentRepository<ClassificationScheme> {

    @Inject
    ClassificationSchemeRepository classificationSchemeRepository
    @Inject
    ClassifierRepository classifierRepository

    @Override
    ClassificationScheme findWithContentById(UUID id) {
        ClassificationScheme classificationScheme = classificationSchemeRepository.findById(id)
        classificationScheme.classifiers = classifierRepository.findAllByClassificationScheme(classificationScheme)
        classificationScheme
    }

    @Override
    ClassificationScheme saveWithContent(@NonNull ClassificationScheme model) {
        ClassificationScheme saved = (ClassificationScheme) super.saveWithContent(model)
        classifierRepository.updateAll(saved.classifiers.findAll{ it.classificationScheme})
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
}
