package org.maurodata.persistence.classifier.dto

import groovy.transform.CompileStatic
import io.micronaut.core.annotation.NonNull
import io.micronaut.core.annotation.Nullable
import io.micronaut.data.annotation.Join
import io.micronaut.data.annotation.Query
import io.micronaut.data.jdbc.annotation.JdbcRepository
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.repository.GenericRepository
import io.micronaut.transaction.annotation.Transactional
import org.maurodata.domain.classifier.ClassificationScheme
import org.maurodata.domain.classifier.Classifier

@CompileStatic
@JdbcRepository(dialect = Dialect.POSTGRES)
abstract class ClassifierDTORepository implements GenericRepository<ClassifierDTO, UUID> {

    @Join(value = 'catalogueUser', type = Join.Type.LEFT_FETCH)
    @Nullable
    abstract ClassifierDTO findById(UUID id)

    @Nullable
    @Join(value = 'catalogueUser', type = Join.Type.LEFT_FETCH)
    @Query('''select * from core.classifier where label = :pathIdentifier AND ((parent_classifier_id IS NOT NULL AND parent_classifier_id = :item) OR (parent_classifier_id IS NULL AND classification_scheme_id = :item))''')
    abstract List<Classifier> findAllByParentAndPathIdentifier(UUID item, String pathIdentifier)

    @Join(value = 'catalogueUser', type = Join.Type.LEFT_FETCH)
    @Nullable
    abstract List<ClassifierDTO> findAllByClassificationScheme(ClassificationScheme classificationScheme)


    @Query(''' insert into core.join_administered_item_to_classifier (catalogue_item_id, catalogue_item_domain_type,classifier_id) values (:catalogueItemId, :catalogueItemDomainType, :classifierId) ''')
    abstract UUID addAdministeredItem(@NonNull UUID catalogueItemId, @NonNull catalogueItemDomainType, @NonNull UUID classifierId)

    @Nullable
    @Query(''' select * from core.classifier c 
               where exists ( select classifier_id from core.join_administered_item_to_classifier jaic 
               where jaic.catalogue_item_id = :catalogueItemId 
               and jaic.catalogue_item_domain_type = :catalogueItemDomainType
               and jaic.classifier_id = c.id 
               and c.id = :classifierId)''')
    abstract Classifier findByAdministeredItemIdAndClassifierId(String catalogueItemDomainType, UUID catalogueItemId, UUID classifierId)

    @Nullable
    @Query(''' select * from core.classifier c 
               where exists ( select classifier_id from core.join_administered_item_to_classifier jaic 
               where jaic.catalogue_item_id = :catalogueItemId 
               and jaic.catalogue_item_domain_type = :catalogueItemDomainType
               and jaic.classifier_id = c.id )''')
    abstract List<Classifier> findAllByAdministeredItem(String catalogueItemDomainType, UUID catalogueItemId)

    @Transactional
    @Query(''' delete from core.join_administered_item_to_classifier jaic where jaic.catalogue_item_id = :catalogueItemId 
               and jaic.catalogue_item_domain_type = :catalogueItemDomainType ''')
    abstract long deleteAllForAdministeredItem(String catalogueItemDomainType, UUID catalogueItemId)

    @Transactional
    @Query(''' delete from core.join_administered_item_to_classifier jaic where jaic.catalogue_item_id = :catalogueItemId
               and jaic.catalogue_item_domain_type = :catalogueItemDomainType
               and jaic.classifier_id = :classifierId
           ''')
    abstract long deleteAdministeredItemClassifier(String catalogueItemDomainType, UUID catalogueItemId, UUID classifierId)

    @Query(''' delete from core.join_administered_item_to_classifier jaic where jaic.classifier_id = :classifierId  ''')
    abstract long deleteAllForClassifier(UUID classifierId)


    @Nullable
    @Join(value = 'catalogueUser', type = Join.Type.LEFT_FETCH)
    @Query('''select * from core.classifier where parent_classifier_id = :classifierId ''')
    abstract List<Classifier> findAllByClassifier(UUID classifierId)

    @Nullable
    @Query('''select * from core.classifier where label like :label ''')
    abstract List<Classifier> findAllByLabelContaining(String label)
}
