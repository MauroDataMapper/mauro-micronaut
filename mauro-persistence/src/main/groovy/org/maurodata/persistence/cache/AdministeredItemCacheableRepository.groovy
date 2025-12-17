package org.maurodata.persistence.cache

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import io.micronaut.cache.annotation.CacheConfig
import io.micronaut.cache.annotation.CacheInvalidate
import io.micronaut.cache.annotation.Cacheable
import io.micronaut.core.annotation.NonNull
import io.micronaut.core.annotation.Nullable
import jakarta.inject.Singleton
import org.maurodata.domain.classifier.Classifier
import org.maurodata.domain.dataflow.DataClassComponent
import org.maurodata.domain.dataflow.DataElementComponent
import org.maurodata.domain.dataflow.DataFlow
import org.maurodata.domain.datamodel.DataClass
import org.maurodata.domain.datamodel.DataElement
import org.maurodata.domain.datamodel.DataModel
import org.maurodata.domain.datamodel.DataType
import org.maurodata.domain.datamodel.EnumerationValue
import org.maurodata.domain.model.AdministeredItem
import org.maurodata.domain.terminology.Term
import org.maurodata.domain.terminology.TermRelationship
import org.maurodata.domain.terminology.TermRelationshipType
import org.maurodata.domain.terminology.Terminology
import org.maurodata.persistence.ContentsService
import org.maurodata.persistence.classifier.ClassifierRepository
import org.maurodata.persistence.dataflow.DataClassComponentRepository
import org.maurodata.persistence.dataflow.DataElementComponentRepository
import org.maurodata.persistence.dataflow.DataFlowRepository
import org.maurodata.persistence.datamodel.DataClassRepository
import org.maurodata.persistence.datamodel.DataElementRepository
import org.maurodata.persistence.datamodel.DataTypeRepository
import org.maurodata.persistence.datamodel.EnumerationValueRepository
import org.maurodata.persistence.datamodel.dto.DataClassExtensionDTO
import org.maurodata.persistence.model.AdministeredItemRepository
import org.maurodata.persistence.terminology.TermRelationshipRepository
import org.maurodata.persistence.terminology.TermRelationshipTypeRepository
import org.maurodata.persistence.terminology.TermRepository

@Slf4j
@CompileStatic
@CacheConfig(cacheNames = 'items-cache', keyGenerator = StringCacheKeyGenerator)
abstract class AdministeredItemCacheableRepository<I extends AdministeredItem> extends ItemCacheableRepository<I> implements AdministeredItemRepository<I> {

    static final String FIND_ALL_BY_PARENT = 'findAll'
    static final String READ_ALL_BY_PARENT = 'readAll'

    AdministeredItemRepository<I> repository

    AdministeredItemCacheableRepository(AdministeredItemRepository<I> itemRepository, ContentsService contentsService) {
        super(itemRepository)
        repository = itemRepository
        this.contentsService = contentsService
    }

    List<I> findAllByParentAndPathIdentifier(UUID item, String pathIdentifier) {
        repository.findAllByParentAndPathIdentifier( item,  pathIdentifier)
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
        mutableCachedLookupByParent(lookup, domainType, parent).collect {it.clone() as I}
    }

    @Cacheable
    List<I> mutableCachedLookupByParent(String lookup, String domainType, AdministeredItem parent) {
        return switch (lookup) {
            case FIND_ALL_BY_PARENT -> repository.findAllByParent(parent)
            case READ_ALL_BY_PARENT -> repository.readAllByParent(parent)
            default -> throw new IllegalStateException()
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

    List<I> findAllByLabel(String label){
        repository.findAllByLabel(label)
    }


    // Cacheable Administered Item Repository definitions

    @CompileStatic
    @Singleton
    static class TermCacheableRepository extends AdministeredItemCacheableRepository<Term> {
        TermCacheableRepository(TermRepository termRepository, ContentsService contentsService) {
            super(termRepository, contentsService)
        }

        // not cached
        List<Term> readChildTermsByParent(UUID terminologyId, @Nullable UUID id) {
            ((TermRepository) repository).readChildTermsByParent(terminologyId, id)
        }

        List<Term> findAllByTerminology(Terminology terminology) {
            ((TermRepository) repository).findAllByTerminology(terminology)
        }

        Term findAllByTerminologyAndCode(Terminology terminology, String code) {
            ((TermRepository) repository).findAllByTerminologyAndCode(terminology, code)
        }

        List<Term> readAllByTerminologyIdIn(Collection<UUID> terminologyIds) {
            ((TermRepository) repository).readAllByTerminologyIdIn(terminologyIds)
        }

        Set<Term> findAllByCodeSetsIdIn(@NonNull List<UUID> uuids) {
            ((TermRepository) repository).findAllByCodeSetsIdIn(uuids)
        }




        @Override
        Boolean handles(Class clazz) {
            repository.handles(clazz)
        }
    }

    @CompileStatic
    @Singleton
    static class TermRelationshipCacheableRepository extends AdministeredItemCacheableRepository<TermRelationship> {
        TermRelationshipCacheableRepository(TermRelationshipRepository termRelationshipRepository, ContentsService contentsService) {
            super(termRelationshipRepository, contentsService)
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

        List<TermRelationship> findAllByTerminologyAndSourceTermOrTargetTerm(Terminology terminology, Term term) {
            ((TermRelationshipRepository) repository).findAllByTerminologyAndSourceTermOrTargetTerm(terminology, term)
        }

        List<TermRelationship> readAllByTerminologyIdIn(Collection<UUID> terminologyIds) {
            ((TermRelationshipRepository) repository).readAllByTerminologyIdIn(terminologyIds)
        }


    }

    @CompileStatic
    @Singleton
    static class TermRelationshipTypeCacheableRepository extends AdministeredItemCacheableRepository<TermRelationshipType> {
        TermRelationshipTypeCacheableRepository(TermRelationshipTypeRepository termRelationshipTypeRepository, ContentsService contentsService) {
            super(termRelationshipTypeRepository, contentsService)
        }

        List<TermRelationshipType> readAllByTerminologyIdIn(Collection<UUID> terminologyIds) {
            ((TermRelationshipTypeRepository) repository).readAllByTerminologyIdIn(terminologyIds)
        }

    }

    @Singleton
    @CompileStatic
    static class DataClassCacheableRepository extends AdministeredItemCacheableRepository<DataClass> {
        DataClassCacheableRepository(DataClassRepository dataClassRepository, ContentsService contentsService) {
            super(dataClassRepository, contentsService)
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
        DataClass readByDataModelAndLabelAndParentDataClassIsNull(DataModel dataModel, String label) {
            ((DataClassRepository) repository).readByDataModelAndLabelAndParentDataClassIsNull(dataModel, label)
        }


        // not cached
        List<DataClass> readAllByDataModel(DataModel dataModel) {
            ((DataClassRepository) repository).readAllByDataModel(dataModel)
        }

        // not cached
        List<DataClass> readAllByDataModelIdInAndParentDataClassIsNull(Collection<UUID> dataModelIds) {
            ((DataClassRepository) repository).readAllByDataModelIdInAndParentDataClassIsNull(dataModelIds)
        }

        // not cached
        List<DataClass> readAllByParentDataClassIdIn(Collection<UUID> dataClassIds) {
            ((DataClassRepository) repository).readAllByParentDataClassIdIn(dataClassIds)
        }

        // not cached
        DataClass readByParentDataClassAndLabel(DataClass parentDataClass, String label) {
            ((DataClassRepository) repository).readByParentDataClassAndLabel(parentDataClass, label)
        }

        // not cached
        DataClass createExtensionRelationship(DataClass sourceDataClass, DataClass targetDataClass) {
            invalidate(sourceDataClass)
            ((DataClassRepository) repository).addDataClassExtensionRelationship(sourceDataClass.id, targetDataClass.id)

        }
        // not cached
        DataClass addDataClassExtensionRelationship(@NonNull UUID dataClassId, @NonNull UUID extendedDataClassId) {
            ((DataClassRepository) repository).addDataClassExtensionRelationship(dataClassId, extendedDataClassId)
        }


        // not cached
        long deleteExtensionRelationship(DataClass sourceDataClass, DataClass targetDataClass) {
            invalidate(sourceDataClass)
            ((DataClassRepository) repository).deleteExtensionRelationship(sourceDataClass.id, targetDataClass.id)
        }

        // not cached
        long deleteExtensionRelationships(List<UUID> dataClassIds) {
            dataClassIds.each {
                invalidate(it)
            }
            ((DataClassRepository) repository).deleteExtensionRelationships(dataClassIds)
        }

        List<DataClassExtensionDTO> getDataClassExtensionRelationships(List<UUID> dataClassIds) {
            ((DataClassRepository) repository).getDataClassExtensionRelationships(dataClassIds)
        }


        @Override
        Boolean handles(String domainType) {
            domainClass.simpleName.equalsIgnoreCase(domainType) || (domainClass.simpleName + 'es').equalsIgnoreCase(domainType)
        }
    }

    @Singleton
    @CompileStatic
    static class DataElementCacheableRepository extends AdministeredItemCacheableRepository<DataElement> {
        DataElementCacheableRepository(DataElementRepository dataElementRepository, ContentsService contentsService) {
            super(dataElementRepository, contentsService)
        }

        // not cached
        List<DataElement> readAllByDataClass_Id(UUID dataClassId) {
            ((DataElementRepository) repository).readAllByDataClassId(dataClassId)
        }

        List<DataElement> readAllByDataTypeIn(List<DataType> dataTypes){
            ((DataElementRepository) repository).readAllByDataTypeIn(dataTypes)
        }


        List<DataElement> readAllByDataModel_Id(UUID dataModelId){
            ((DataElementRepository) repository).readAllByDataModelId(dataModelId)
        }

        DataElement readByDataClassAndLabel(DataClass dataClass, String label) {
            ((DataElementRepository) repository).readByDataClassAndLabel(dataClass, label)
        }

        List<DataElement> readAllByDataClassIdIn(List<UUID> dataClassIds){
            ((DataElementRepository) repository).readAllByDataClassIdIn(dataClassIds)
        }

        List<DataElement> findAllByDataClass(DataClass dataClass){
            ((DataElementRepository) repository).findAllByDataClass(dataClass)
        }

    }

    @Singleton
    @CompileStatic
    static class DataTypeCacheableRepository extends AdministeredItemCacheableRepository<DataType> {
        DataTypeCacheableRepository(DataTypeRepository dataTypeRepository, ContentsService contentsService) {
            super(dataTypeRepository, contentsService)
        }

        @Nullable
        List<DataType> findAllByReferenceClass(DataClass referenceClass) {
            ((DataTypeRepository) repository).findAllByReferenceClass(referenceClass.id)
        }

        @Nullable
        List<DataType> findByReferenceClassIn(List<DataClass> dataClasses) {
            ((DataTypeRepository) repository).findByReferenceClassIn(dataClasses.id)
        }
        @Override
        Boolean handles(String domainType) {
            repository.handles(domainType)
        }

        List<DataType> readAllByDataModelIdIn(Collection<UUID> dataModelIds) {
            ((DataTypeRepository) repository).readAllByDataModelIdIn(dataModelIds)
        }
    }

    @Singleton
    @CompileStatic
    static class EnumerationValueCacheableRepository extends AdministeredItemCacheableRepository<EnumerationValue> {
        EnumerationValueCacheableRepository(EnumerationValueRepository enumerationValueRepository, ContentsService contentsService) {
            super(enumerationValueRepository, contentsService)
        }

        // not cached
        List<EnumerationValue> readAllByEnumerationType_Id(UUID enumerationTypeId) {
            ((EnumerationValueRepository) repository).readAllByEnumerationTypeId(enumerationTypeId)
        }

        List<EnumerationValue> readAllByEnumerationTypeIdIn(Collection<UUID> dataTypeIds) {
            ((EnumerationValueRepository) repository).readAllByEnumerationTypeIdIn(dataTypeIds)
        }


    }

    @Singleton
    @CompileStatic
    static class ClassifierCacheableRepository extends AdministeredItemCacheableRepository<Classifier> {
        ClassifierCacheableRepository(ClassifierRepository classifierRepository, ContentsService contentsService) {
            super(classifierRepository, contentsService)
        }

        List<Classifier> readAllByParentClassifier_Id(UUID parentClassifierId) {
            ((ClassifierRepository) repository).readAllByParentClassifier_Id(parentClassifierId)
        }

        List<Classifier> readAllByClassificationScheme_Id(UUID classificationSchemeId) {
            ((ClassifierRepository) repository).readAllByClassificationScheme_Id(classificationSchemeId)
        }

        List<Classifier> readAllByClassificationSchemeIdIn(Collection<UUID> classificationSchemeIds) {
            ((ClassifierRepository) repository).readAllByClassificationSchemeIdIn(classificationSchemeIds)
        }

        Long deleteAllJoinAdministeredItemToClassifierIds(Collection<UUID> classifierIds) {
            ((ClassifierRepository) repository).deleteAllJoinAdministeredItemToClassifierIds(classifierIds)
        }

        // not cached
        void addAdministeredItem(AdministeredItem administeredItem, Classifier classifier) {
            ((ClassifierRepository) repository).addAdministeredItem(administeredItem, classifier)

            invalidate(classifier)

            if (administeredItem?.id) {
                invalidateCachedLookupById(FIND_BY_ID, administeredItem.domainType, administeredItem.id)
            }

            if (administeredItem?.parent?.id) {
                invalidateCachedLookupById(FIND_ALL_BY_PARENT, administeredItem.parent.domainType,
                        administeredItem.parent.id)
            }
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

        List<Classifier> findAll(){
            ( ((ClassifierRepository) repository).findAll())
        }

    }

    @Singleton
    @CompileStatic
    static class DataFlowCacheableRepository extends AdministeredItemCacheableRepository<DataFlow> {
        DataFlowCacheableRepository(DataFlowRepository dataFlowRepository, ContentsService contentsService) {
            super(dataFlowRepository, contentsService)
        }

        List<DataFlow> findAllByTarget(DataModel dataModel) {
            ((DataFlowRepository) repository).findAllByTarget(dataModel) as List<DataFlow>
        }

        List<DataFlow> findAllBySource(DataModel dataModel) {
            ((DataFlowRepository) repository).findAllBySource(dataModel) as List<DataFlow>
        }

        @Override
        Boolean handles(String domainType) {
            domainClass.simpleName.equalsIgnoreCase(domainType) || (domainClass.simpleName + 's').equalsIgnoreCase(domainType)
        }
    }

    @Singleton
    @CompileStatic
    static class DataClassComponentCacheableRepository extends AdministeredItemCacheableRepository<DataClassComponent> {
        DataClassComponentCacheableRepository(DataClassComponentRepository dataClassComponentRepository, ContentsService contentsService) {
            super(dataClassComponentRepository, contentsService)
        }
        DataClassComponent addTargetDataClass(@NonNull UUID id, @NonNull UUID dataClassId) {
            invalidate(id)
            ((DataClassComponentRepository) repository).addTargetDataClass(id, dataClassId)
        }
        DataClassComponent addSourceDataClass(@NonNull UUID id, @NonNull UUID dataClassId) {
            invalidate(id)
            ((DataClassComponentRepository) repository).addSourceDataClass(id, dataClassId)
        }
        Long removeTargetDataClass(UUID id, UUID dataClassId) {
            invalidate(id)
            ((DataClassComponentRepository) repository).removeTargetDataClass(id, dataClassId)
        }
        Long removeSourceDataClass(UUID id, UUID dataClassId) {
            invalidate(id)
            ((DataClassComponentRepository) repository).removeSourceDataClass(id, dataClassId)
        }

         List<DataClass> findAllSourceDataClasses(UUID id) {
             ((DataClassComponentRepository) repository).findAllSourceDataClasses(id)
        }

        List<DataClass> findAllTargetDataClasses(UUID id) {
            ((DataClassComponentRepository) repository).findAllTargetDataClasses(id)
        }

        Long removeSourceDataClasses(UUID id) {
            invalidate(id)
            ((DataClassComponentRepository) repository).removeSourceDataClasses(id)
        }

        Long removeTargetDataClasses(UUID id) {
            invalidate(id)
            ((DataClassComponentRepository) repository).removeTargetDataClasses(id)
        }
        Boolean handles(String domainType) {
            domainClass.simpleName.equalsIgnoreCase(domainType) || (domainClass.simpleName + 's').equalsIgnoreCase(domainType)
        }

        List<DataClassComponent> readAllByDataFlowIdIn(List<UUID> dataFlowIds) {
            ((DataClassComponentRepository) repository).readAllByDataFlowIdIn(dataFlowIds)
        }
    }

    @Singleton
    @CompileStatic
    static class DataElementComponentCacheableRepository extends AdministeredItemCacheableRepository<DataElementComponent> {
        DataElementComponentCacheableRepository(DataElementComponentRepository dataElementComponentRepository, ContentsService contentsService) {
            super(dataElementComponentRepository, contentsService)
        }

        DataElementComponent addTargetDataElement(@NonNull UUID id, @NonNull UUID dataElementId) {
            invalidate(id)
            ((DataElementComponentRepository) repository).addTargetDataElement(id, dataElementId)
        }
        DataElementComponent addSourceDataElement(@NonNull UUID id, @NonNull UUID dataElementId) {
            invalidate(id)
            ((DataElementComponentRepository) repository).addSourceDataElement(id, dataElementId)
        }

        Long removeTargetDataElement(UUID id, UUID dataElementId) {
            invalidate(id)
            ((DataElementComponentRepository) repository).removeTargetDataElement(id, dataElementId)
        }
        Long removeTargetDataElements(UUID id) {
            invalidate(id)
            ((DataElementComponentRepository) repository).removeTargetDataElements(id)
        }

        Long removeSourceDataElement(UUID id, UUID dataElementId) {
            invalidate(id)
            ((DataElementComponentRepository) repository).removeSourceDataElement(id, dataElementId)
        }
        Long removeSourceDataElements(UUID id) {
            invalidate(id)
            ((DataElementComponentRepository) repository).removeSourceDataElements(id)
        }

        List<DataElement> getSourceDataElements(UUID id) {
            ((DataElementComponentRepository) repository).getSourceDataElements(id)
        }

        List<DataElement> getTargetDataElements(UUID id) {
            ((DataElementComponentRepository) repository).getTargetDataElements(id)
        }

        List<DataElementComponent> readAllByDataClassComponentIdIn(List<UUID> dataClassComponentIds) {
            ((DataElementComponentRepository) repository).readAllByDataClassComponentIdIn(dataClassComponentIds)
        }


        @Override
        Boolean handles(String domainType) {
            domainClass.simpleName.equalsIgnoreCase(domainType) || (domainClass.simpleName + 's').equalsIgnoreCase(domainType)
        }
    }
}
