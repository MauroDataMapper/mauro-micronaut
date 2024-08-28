package uk.ac.ox.softeng.mauro.persistence.classifier

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import io.micronaut.core.annotation.Nullable
import io.micronaut.data.jdbc.annotation.JdbcRepository
import io.micronaut.data.model.query.builder.sql.Dialect
import jakarta.inject.Inject
import uk.ac.ox.softeng.mauro.domain.classifier.ClassificationScheme
import uk.ac.ox.softeng.mauro.domain.classifier.Classifier
import uk.ac.ox.softeng.mauro.domain.model.AdministeredItem
import uk.ac.ox.softeng.mauro.persistence.classifier.dto.ClassifierDTORepository
import uk.ac.ox.softeng.mauro.persistence.model.ModelItemRepository

@Slf4j
@CompileStatic
@JdbcRepository(dialect = Dialect.POSTGRES)
abstract class ClassifierRepository implements ModelItemRepository<Classifier> {

    @Inject
    ClassifierDTORepository classifierDTORepository

    @Override
    @Nullable
    Classifier findById(UUID id) {
        classifierDTORepository.findById(id)
    }

    @Nullable
    List<Classifier> findAllByClassificationScheme(ClassificationScheme classificationScheme) {
        classifierDTORepository.findAllByClassificationScheme(classificationScheme) as List<Classifier>
    }

    @Override
    @Nullable
    List<Classifier> findAllByParent(AdministeredItem parent) {
        findAllByClassificationScheme((ClassificationScheme) parent)
    }

    @Nullable
    UUID addAdministeredItem( AdministeredItem administeredItem, UUID classifierId){
        log.debug("Adding to joinAdministeredItemToClassifier : administered item $administeredItem.id , classifier: $classifierId")
        classifierDTORepository.addAdministeredItem(administeredItem.id, administeredItem.domainType, classifierId)
    }

    @Nullable
    Classifier findByAdministeredItemAndClassifier(String administeredItemDomainType, UUID administeredItemId, UUID id) {
        classifierDTORepository.findByAdministeredItemIdAndClassifierId(administeredItemDomainType, administeredItemId, id)
    }

    @Nullable
    List<Classifier> findAllForAdministeredItem(String administeredItemDomainType, UUID administeredItemId) {
        classifierDTORepository.findAllByAdministeredItem(administeredItemDomainType, administeredItemId)
    }


    Long deleteAllForAdministeredItem(String administeredItemDomainType, UUID administeredItemId) {
        classifierDTORepository.deleteAllForAdministeredItem(administeredItemDomainType, administeredItemId)
    }

    @Nullable
    abstract List<Classifier> readAllByClassificationScheme(ClassificationScheme classificationScheme)

    @Nullable
    abstract List<Classifier> readAllByClassificationScheme_Id(UUID classificationSchemeId)

    @Nullable
    abstract List<Classifier> readAllByParentClassifier_Id(UUID classifierId)

    @Override
    @Nullable
    List<Classifier> readAllByParent(AdministeredItem parent) {
        readAllByClassificationScheme((ClassificationScheme) parent)
    }

    abstract Long deleteByClassificationSchemeId(UUID classificationSchemeId)

    Long deleteAdministeredItemClassifier(AdministeredItem administeredItem, UUID classifierId) {
        classifierDTORepository.deleteAdministeredItemClassifier(administeredItem.domainType, administeredItem.id, classifierId)
    }

    Long deleteAllAdministeredItemsForClassifier(Classifier classifier){
        classifierDTORepository.deleteAllForClassifier(classifier.id)
    }


    //    @Override
    Long deleteByOwnerId(UUID ownerId) {
        deleteByClassificationSchemeId(ownerId)
    }

    @Override
    Class getDomainClass() {
        Classifier
    }

    @Override
    Boolean handles(Class clazz) {
        domainClass.isAssignableFrom(clazz)
    }

    List<Classifier> findAllByParent(Classifier classifier){
        classifierDTORepository.findAllByClassifier(classifier.id)
    }

    @Override
    Classifier findWithContentById(UUID id, AdministeredItem parent) {
        Classifier classifier = findById(id)
        classifier.childClassifiers = findAllByParent(classifier)
        classifier
     }
}

