package org.maurodata.persistence.classifier

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import io.micronaut.core.annotation.Nullable
import io.micronaut.data.annotation.Query
import io.micronaut.data.jdbc.annotation.JdbcRepository
import io.micronaut.data.model.query.builder.sql.Dialect
import jakarta.inject.Inject
import org.maurodata.domain.classifier.ClassificationScheme
import org.maurodata.domain.classifier.Classifier
import org.maurodata.domain.model.AdministeredItem
import org.maurodata.persistence.classifier.dto.ClassifierDTORepository
import org.maurodata.persistence.model.ModelItemRepository

@Slf4j
@CompileStatic
@JdbcRepository(dialect = Dialect.POSTGRES)
abstract class ClassifierRepository implements ModelItemRepository<Classifier> {

    @Inject
    ClassifierDTORepository classifierDTORepository

    abstract List<Classifier> readAllByClassificationSchemeIdIn(Collection<UUID> classificationSchemeIds)

    @Override
    @Nullable
    Classifier findById(UUID id) {
        classifierDTORepository.findById(id) as Classifier
    }

    @Override
    @Nullable
    List<Classifier> findAllByParentAndPathIdentifier(UUID item,String pathIdentifier) {
        classifierDTORepository.findAllByParentAndPathIdentifier(item,pathIdentifier) as List<Classifier>
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


    @Override
    List<Classifier> findAllByLabel(String label) {
        classifierDTORepository.findAllByLabel(label)
    }
    @Nullable
    UUID addAdministeredItem(AdministeredItem administeredItem, Classifier classifier) {
        log.debug("Adding to joinAdministeredItemToClassifier : administered item $administeredItem.id , classifier: $classifier.id")
        classifierDTORepository.addAdministeredItem(administeredItem.id, administeredItem.domainType, classifier.id)
    }

    @Nullable
    Classifier findByAdministeredItemAndClassifier(String administeredItemDomainType, UUID administeredItemId, UUID id) {
        classifierDTORepository.findByAdministeredItemIdAndClassifierId(administeredItemDomainType, administeredItemId, id)
    }

    @Nullable
    List<Classifier> findAllForAdministeredItem(String administeredItemDomainType, UUID administeredItemId) {
        classifierDTORepository.findAllByAdministeredItem(administeredItemDomainType, administeredItemId) as List<Classifier>
    }

    @Nullable
    abstract List<Classifier> findAll()


    @Nullable
    abstract List<Classifier> readAllByClassificationScheme(ClassificationScheme classificationScheme)


    @Nullable
    abstract List<Classifier> readAllByParentClassifier_Id(UUID classifierId)

    @Override
    @Nullable
    List<Classifier> readAllByParent(AdministeredItem parent) {
        readAllByClassificationScheme((ClassificationScheme) parent)
    }

    @Nullable
    abstract List<Classifier> readAllByClassificationScheme_Id(UUID classificationSchemeId)


    abstract Long deleteByClassificationSchemeId(UUID classificationSchemeId)

    Long deleteJoinAdministeredItemToClassifier(AdministeredItem administeredItem, UUID classifierId) {
        classifierDTORepository.deleteAdministeredItemClassifier(administeredItem.domainType, administeredItem.id, classifierId)
    }

    @Query('''delete from core.join_administered_item_to_classifier jaic where jaic.classifier_id in (:classifierIds)''')
    abstract Long deleteAllJoinAdministeredItemToClassifierIds(Collection<UUID> classifierIds)

    Long deleteAllJoinAdministeredItemToClassifier(Classifier classifier) {
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
        return domainType != null && domainType.toLowerCase() in ['classifier', 'classifiers']
    }

    List<Classifier> findAllByParent(Classifier classifier) {
        classifierDTORepository.findAllByClassifier(classifier.id) as List<Classifier>
    }

    List<Classifier> readAll(){
        findAll()
    }


    Boolean handlesPathPrefix(final String pathPrefix) {
        'cl'.equalsIgnoreCase(pathPrefix)
    }
}

