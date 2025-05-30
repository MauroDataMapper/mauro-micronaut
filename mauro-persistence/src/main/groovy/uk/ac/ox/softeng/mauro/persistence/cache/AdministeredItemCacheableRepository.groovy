package uk.ac.ox.softeng.mauro.persistence.cache

import uk.ac.ox.softeng.mauro.domain.classifier.Classifier
import uk.ac.ox.softeng.mauro.domain.dataflow.DataClassComponent
import uk.ac.ox.softeng.mauro.domain.datamodel.DataClass
import uk.ac.ox.softeng.mauro.domain.datamodel.DataElement
import uk.ac.ox.softeng.mauro.domain.datamodel.DataModel
import uk.ac.ox.softeng.mauro.domain.datamodel.DataType
import uk.ac.ox.softeng.mauro.domain.datamodel.EnumerationValue
import uk.ac.ox.softeng.mauro.domain.model.AdministeredItem
import uk.ac.ox.softeng.mauro.domain.terminology.Term
import uk.ac.ox.softeng.mauro.domain.terminology.TermRelationship
import uk.ac.ox.softeng.mauro.domain.terminology.TermRelationshipType
import uk.ac.ox.softeng.mauro.persistence.classifier.ClassifierRepository
import uk.ac.ox.softeng.mauro.persistence.dataflow.DataClassComponentRepository
import uk.ac.ox.softeng.mauro.persistence.datamodel.DataClassRepository
import uk.ac.ox.softeng.mauro.persistence.datamodel.DataElementRepository
import uk.ac.ox.softeng.mauro.persistence.datamodel.DataTypeRepository
import uk.ac.ox.softeng.mauro.persistence.datamodel.EnumerationValueRepository
import uk.ac.ox.softeng.mauro.persistence.model.AdministeredItemRepository
import uk.ac.ox.softeng.mauro.persistence.terminology.TermRelationshipRepository
import uk.ac.ox.softeng.mauro.persistence.terminology.TermRelationshipTypeRepository
import uk.ac.ox.softeng.mauro.persistence.terminology.TermRepository

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import io.micronaut.cache.annotation.CacheConfig
import io.micronaut.cache.annotation.CacheInvalidate
import io.micronaut.cache.annotation.Cacheable
import io.micronaut.core.annotation.NonNull
import io.micronaut.core.annotation.Nullable
import jakarta.inject.Singleton

@Slf4j
@CompileStatic
@CacheConfig(cacheNames = 'items-cache', keyGenerator = StringCacheKeyGenerator)
abstract class AdministeredItemCacheableRepository<I extends AdministeredItem> extends ItemCacheableRepository<I> implements AdministeredItemRepository<I> {

    static final String FIND_ALL_BY_PARENT = 'findAll'
    static final String READ_ALL_BY_PARENT = 'readAll'

    AdministeredItemRepository<I> repository

    AdministeredItemCacheableRepository(AdministeredItemRepository<I> itemRepository) {
        super(itemRepository)
        repository = itemRepository
    }

    List<I> findAllByParent(AdministeredItem parent) {
        cachedLookupByParent(FIND_ALL_BY_PARENT, domainType, parent)
    }

    List<I> readAllByParent(AdministeredItem parent) {
        cachedLookupByParent(READ_ALL_BY_PARENT, domainType, parent)
    }

    I update(I oldItem, I newItem) {
        I updated = repository.update(newItem)
        invalidate(oldItem, newItem)
        updated
    }

    List<I> cachedLookupByParent(String lookup, String domainType, AdministeredItem parent) {
        mutableCachedLookupByParent(lookup, domainType, parent).collect {it.clone()}
    }

    @Cacheable
    List<I> mutableCachedLookupByParent(String lookup, String domainType, AdministeredItem parent) {
        switch (lookup) {
            case FIND_ALL_BY_PARENT -> repository.findAllByParent(parent)
            case READ_ALL_BY_PARENT -> repository.readAllByParent(parent)
        }
    }

    @CacheInvalidate
    void invalidateCachedLookupByParent(String lookup, String domainType, AdministeredItem parent) {
        null
    }

    @Override
    void invalidate(I item) {
        super.invalidate(item)
        // Invalidate collections that could contain the new item
        AdministeredItem parent = item.parent
        log.debug "Invalidating parent of $item, parent is $item.parent"
        invalidateCachedLookupByParent(FIND_ALL_BY_PARENT, domainType, parent)
        invalidateCachedLookupByParent(READ_ALL_BY_PARENT, domainType, parent)
    }

    void invalidate(I oldItem, I newItem) {
        if (oldItem.parent && oldItem.parent.id != newItem.parent?.id) {
            invalidate(oldItem)
        }
        invalidate(newItem)
    }


    // Cacheable Administered Item Repository definitions

    @CompileStatic
    @Singleton
    static class TermCacheableRepository extends AdministeredItemCacheableRepository<Term> {
        TermCacheableRepository(TermRepository termRepository) {
            super(termRepository)
        }

        // not cached
        List<Term> readChildTermsByParent(UUID terminologyId, @Nullable UUID id) {
            ((TermRepository) repository).readChildTermsByParent(terminologyId, id)
        }
    }

    @CompileStatic
    @Singleton
    static class TermRelationshipCacheableRepository extends AdministeredItemCacheableRepository<TermRelationship> {
        TermRelationshipCacheableRepository(TermRelationshipRepository termRelationshipRepository) {
            super(termRelationshipRepository)
        }

        // not cached

        List<TermRelationship> readAllBySourceTerm(Term sourceTerm) {
            ((TermRelationshipRepository) repository).readAllBySourceTerm(sourceTerm)
        }

        List<TermRelationship> readAllByTargetTerm(Term targetTerm) {
            ((TermRelationshipRepository) repository).readAllByTargetTerm(targetTerm)
        }

        List<TermRelationship> readAllByRelationshipType(TermRelationshipType relationshipType) {
            ((TermRelationshipRepository) repository).readAllByRelationshipType(relationshipType)
        }
    }

    @CompileStatic
    @Singleton
    static class TermRelationshipTypeCacheableRepository extends AdministeredItemCacheableRepository<TermRelationshipType> {
        TermRelationshipTypeCacheableRepository(TermRelationshipTypeRepository termRelationshipTypeRepository) {
            super(termRelationshipTypeRepository)
        }
    }

    @Singleton
    @CompileStatic
    static class DataClassCacheableRepository extends AdministeredItemCacheableRepository<DataClass> {
        DataClassCacheableRepository(DataClassRepository dataClassRepository) {
            super(dataClassRepository)
        }

        // not cached
        List<DataClass> readAllByParentDataClass_Id(UUID parentDataClassId) {
            ((DataClassRepository) repository).readAllByParentDataClass_Id(parentDataClassId)
        }

        // not cached
        List<DataClass> readAllByParentDataClass(DataClass parentDataClass) {
            ((DataClassRepository) repository).readAllByParentDataClass(parentDataClass)
        }

        // not cached
        List<DataClass> readAllByDataModelAndParentDataClassIsNull(DataModel dataModel) {
            ((DataClassRepository) repository).readAllByDataModelAndParentDataClassIsNull(dataModel)
        }

        // not cached
        DataClass createExtensionRelationship(DataClass sourceDataClass, DataClass targetDataClass) {
            invalidate(sourceDataClass)
            ((DataClassRepository) repository).addDataClassExtensionRelationship(sourceDataClass.id, targetDataClass.id)

        }

        // not cached
        long deleteExtensionRelationship(DataClass sourceDataClass, DataClass targetDataClass) {
            invalidate(sourceDataClass)
            ((DataClassRepository) repository).deleteExtensionRelationship(sourceDataClass.id, targetDataClass.id)
        }


        @Override
        Boolean handles(String domainType) {
            domainClass.simpleName.equalsIgnoreCase(domainType) || (domainClass.simpleName + 'es').equalsIgnoreCase(domainType)
        }
    }

    @Singleton
    @CompileStatic
    static class DataElementCacheableRepository extends AdministeredItemCacheableRepository<DataElement> {
        DataElementCacheableRepository(DataElementRepository dataElementRepository) {
            super(dataElementRepository)
        }

        // not cached
        List<DataElement> readAllByDataClass_Id(UUID dataClassId) {
            ((DataElementRepository) repository).readAllByDataClassId(dataClassId)
        }

        List<DataElement> readAllByDataType(DataType dataType) {
            ((DataElementRepository) repository).readAllByDataType(dataType)
        }
    }

    @Singleton
    @CompileStatic
    static class DataTypeCacheableRepository extends AdministeredItemCacheableRepository<DataType> {
        DataTypeCacheableRepository(DataTypeRepository dataTypeRepository) {
            super(dataTypeRepository)
        }

        @Nullable
        List<DataType> findAllByReferenceClassId(UUID referenceClassId) {
            ((DataTypeRepository) repository).findAllByReferenceClassId(referenceClassId).unique()
        }

        @Override
        Boolean handles(String domainType) {
            repository.handles(domainType)
        }

    }

    @Singleton
    @CompileStatic
    static class EnumerationValueCacheableRepository extends AdministeredItemCacheableRepository<EnumerationValue> {
        EnumerationValueCacheableRepository(EnumerationValueRepository enumerationValueRepository) {
            super(enumerationValueRepository)
        }

        // not cached
        List<EnumerationValue> readAllByEnumerationType_Id(UUID enumerationTypeId) {
            ((EnumerationValueRepository) repository).readAllByEnumerationTypeId(enumerationTypeId)
        }

    }

    @Singleton
    @CompileStatic
    static class ClassifierCacheableRepository extends AdministeredItemCacheableRepository<Classifier> {
        ClassifierCacheableRepository(ClassifierRepository classifierRepository) {
            super(classifierRepository)
        }

        List<Classifier> readAllByParentClassifier_Id(UUID parentClassifierId) {
            ((ClassifierRepository) repository).readAllByParentClassifier_Id(parentClassifierId)
        }

        List<Classifier> readAllByClassificationScheme_Id(UUID classificationSchemeId) {
            ((ClassifierRepository) repository).readAllByClassificationScheme_Id(classificationSchemeId)
        }

        // not cached
        UUID addAdministeredItem(AdministeredItem administeredItem, Classifier classifier) {
            ((ClassifierRepository) repository).addAdministeredItem(administeredItem, classifier)

            invalidate(classifier)

            if (administeredItem?.id)
                invalidateCachedLookupById(FIND_BY_ID, administeredItem.domainType, administeredItem.id)

            if (administeredItem?.parent?.id)
                invalidateCachedLookupById(FIND_ALL_BY_PARENT, administeredItem.parent.domainType,
                        administeredItem.parent.id)
        }

        // not cached
        Classifier findByAdministeredItemAndClassifier(String administeredItemDomainType, UUID administeredItemId, UUID classifierId) {
            ((ClassifierRepository) repository).findByAdministeredItemAndClassifier(administeredItemDomainType, administeredItemId,
                    classifierId)
        }

        // not cached
        @Nullable
        List<Classifier> findAllForAdministeredItem(AdministeredItem administeredItem) {
            ((ClassifierRepository) repository).findAllForAdministeredItem(administeredItem.domainType, administeredItem.id)
        }

        // not cached
        Long deleteJoinAdministeredItemToClassifier(AdministeredItem administeredItem, UUID classifierId) {
            Long deleted = ((ClassifierRepository) repository).deleteJoinAdministeredItemToClassifier(administeredItem, classifierId)

            if (administeredItem?.id)
                invalidateCachedLookupById(FIND_BY_ID, administeredItem.domainType, administeredItem.id)

            if (administeredItem?.parent?.id)
                invalidateCachedLookupById(FIND_ALL_BY_PARENT, administeredItem.parent.domainType,
                        administeredItem.parent.id)
            deleted
        }

    }

    @Singleton
    @CompileStatic
    static class DataClassComponentCacheableRepository extends AdministeredItemCacheableRepository<DataClassComponent> {
        DataClassComponentCacheableRepository(DataClassComponentRepository dataClassComponentRepository) {
            super(dataClassComponentRepository)
        }

        DataClassComponent addTargetDataClass(@NonNull UUID id, @NonNull UUID dataClassId) {
            ((DataClassComponentRepository) repository).addTargetDataClass(id, dataClassId)
        }
        DataClassComponent addSourceDataClass(@NonNull UUID id, @NonNull UUID dataClassId) {
            ((DataClassComponentRepository) repository).addSourceDataClass(id, dataClassId)
        }

        Long removeTargetDataClass(UUID id, UUID dataClassId) {
            ((DataClassComponentRepository) repository).removeTargetDataClass(id, dataClassId)
        }
        Long removeSourceDataClass(UUID id, UUID dataClassId) {
            ((DataClassComponentRepository) repository).removeSourceDataClass(id, dataClassId)
        }

        @Override
        Boolean handles(String domainType) {
            domainClass.simpleName.equalsIgnoreCase(domainType) || (domainClass.simpleName + 's').equalsIgnoreCase(domainType)
        }
    }
}
