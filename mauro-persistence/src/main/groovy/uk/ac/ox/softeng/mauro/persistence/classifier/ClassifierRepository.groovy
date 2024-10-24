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
        classifierDTORepository.findById(id) as Classifier
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
    UUID addAdministeredItem( AdministeredItem administeredItem, Classifier classifier){
        log.debug("Adding to joinAdministeredItemToClassifier : administered item $administeredItem.id , classifier: $classifier.id")
        classifierDTORepository.addAdministeredItem(administeredItem.id, administeredItem.domainType, classifier.id)
    }

    @Nullable
    Classifier findByAdministeredItemAndClassifier(String administeredItemDomainType, UUID administeredItemId, UUID id) {
        classifierDTORepository.findByAdministeredItemIdAndClassifierId(administeredItemDomainType, administeredItemId, id)
    }

    @Nullable
    List<Classifier> findAllForAdministeredItem(String administeredItemDomainType, UUID administeredItemId) {
        classifierDTORepository.findAllByAdministeredItem(administeredItemDomainType, administeredItemId)
    }



    @Nullable
    abstract List<Classifier> readAllByClassificationScheme(ClassificationScheme classificationScheme)


    @Nullable
    abstract List<Classifier> readAllByParentClassifier_Id(UUID classifierId)

    @Override
    @Nullable
    List<Classifier> readAllByParent(AdministeredItem parent) {
        readAllByClassificationScheme((ClassificationScheme) parent)
    }

    abstract Long deleteByClassificationSchemeId(UUID classificationSchemeId)

    Long deleteJoinAdministeredItemToClassifier(AdministeredItem administeredItem, UUID classifierId) {
        classifierDTORepository.deleteAdministeredItemClassifier(administeredItem.domainType, administeredItem.id, classifierId)
    }

    Long deleteAllJoinAdministeredItemToClassifier(Classifier classifier){
        classifierDTORepository.deleteAllForClassifier(classifier.id)
    }


    @Override
    Class getDomainClass() {
        Classifier
    }

    @Override
    Boolean handles(Class clazz) {
        domainClass.isAssignableFrom(clazz)
    }


    @Override
    Boolean handles(String domainType) {
        domainType.toLowerCase() in ['classifier', 'classifiers']
    }
    List<Classifier> findAllByParent(Classifier classifier){
        classifierDTORepository.findAllByClassifier(classifier.id)
    }
}

