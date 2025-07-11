package org.maurodata.persistence.datamodel

import io.micronaut.http.HttpStatus
import org.maurodata.ErrorHandler
import org.maurodata.domain.datamodel.DataClass
import org.maurodata.domain.datamodel.DataElement
import org.maurodata.domain.datamodel.DataType
import org.maurodata.domain.model.AdministeredItem
import org.maurodata.persistence.cache.AdministeredItemCacheableRepository
import org.maurodata.persistence.cache.AdministeredItemCacheableRepository.DataClassCacheableRepository

import groovy.transform.CompileStatic
import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.maurodata.persistence.model.AdministeredItemContentRepository

@CompileStatic
@Singleton
class DataClassContentRepository extends AdministeredItemContentRepository {

    DataClassCacheableRepository dataClassCacheableRepository

    AdministeredItemCacheableRepository.DataElementCacheableRepository dataElementCacheableRepository

    AdministeredItemCacheableRepository.DataTypeCacheableRepository dataTypeCacheableRepository

    DataClassContentRepository(DataClassCacheableRepository dataClassCacheableRepository,
                               AdministeredItemCacheableRepository.DataElementCacheableRepository dataElementCacheableRepository,
                               AdministeredItemCacheableRepository.DataTypeCacheableRepository dataTypeCacheableRepository) {
        this.dataClassCacheableRepository = dataClassCacheableRepository
        this.dataElementCacheableRepository = dataElementCacheableRepository
        this.dataTypeCacheableRepository = dataTypeCacheableRepository
    }

    @Inject
    DataClassRepository dataClassRepository

    @Override
    DataClass readWithContentById(UUID id) {
        DataClass dataClass = dataClassCacheableRepository.findById(id)
        dataClass.dataClasses = dataClassCacheableRepository.readAllByParentDataClass_Id(id)
        dataClass.dataClasses.collect {
            it.dataElements = dataElementCacheableRepository.readAllByDataClass_Id(it.id)
            it.dataElements = getDataTypes(it)
            DataClass dcWithContent = readWithContentById(it.id)
            it.dataClasses = dcWithContent.dataClasses
        }
        dataClass.dataElements = dataElementCacheableRepository.readAllByDataClass_Id(id)
        dataClass.dataElements = getDataTypes(dataClass)

        dataClass.extendsDataClasses = dataClassRepository.getDataClassExtensionRelationships(id)
        dataClass.extendsDataClasses.each {it.extendedBy.add(dataClass)}

        dataClass.referenceTypes = dataTypeCacheableRepository.findAllByReferenceClass(dataClass)
        dataClass
    }

    @Override
    DataClass saveWithContent(AdministeredItem administeredItem) {
        DataClass saved = (DataClass) super.saveWithContent(administeredItem)
        DataClass dataClass = (DataClass) administeredItem
        Map<String, DataClass> dataClassMap = [:]
        if (dataClass.extendsDataClasses.find {it.label && !it.id}) {
            dataClassRepository.findAllByDataModel(dataClass.dataModel).each {
                dataClassMap[it.label] = it
            }
        }
        dataClass.extendsDataClasses.each {extendedDataClass ->
            if(extendedDataClass.id) {
                dataClassRepository.addDataClassExtensionRelationship(saved.id, extendedDataClass.id)
            }
            else if(extendedDataClass.label) {
                if (dataClassMap[extendedDataClass.label]) {
                    UUID extendedDataClassId = dataClassMap[extendedDataClass.label].id
                    dataClassRepository.addDataClassExtensionRelationship(saved.id, extendedDataClassId)
                }
            }
        }
        saved
    }

    @Override
    Long deleteWithContent(AdministeredItem administeredItem) {
        Map<String, DataClass> deletedDataClassLookup = [:]
        deletedDataClassLookup.put(administeredItem.label, administeredItem as DataClass)
        if ((administeredItem as DataClass).dataElements) {
            deleteDataElements((administeredItem as DataClass).dataElements)
        }
        if ((administeredItem as DataClass).dataClasses) {
           deleteChildClasses((administeredItem as DataClass).dataClasses, deletedDataClassLookup)
        }
        deleteDanglingReferenceTypes(deletedDataClassLookup)
        dataClassCacheableRepository.delete(administeredItem as DataClass)
    }

    protected Long deleteDataElements(List<DataElement> dataElements) {
        dataElementCacheableRepository.deleteAll(dataElements)
    }
 
    protected Long deleteChildClasses(List<DataClass> children, Map<String, DataClass> deletedDataClassLookup) {
        children.each {
            if (it.dataClasses) {
                deleteChildClasses(it.dataClasses, deletedDataClassLookup)
            }
            if (it.dataElements){
                deleteDataElements(it.dataElements)
            }
            deletedDataClassLookup.put(it.label, it)
        }
        dataClassCacheableRepository.deleteAll(children)
    }

    protected void deleteDanglingReferenceTypes(Map<String, DataClass> deletedDataClassLookup) {
        List<DataType> dataTypes = dataTypeCacheableRepository.findByReferenceClassIn(deletedDataClassLookup.values() as List<DataClass>).unique() as List<DataType>
        List<DataElement> referencedDataElements = dataElementCacheableRepository.readAllByDataTypeIn(dataTypes)
        if (!referencedDataElements.isEmpty()){
            ErrorHandler.handleError(HttpStatus.UNPROCESSABLE_ENTITY, "DataClass(es) referenced as ReferencedDataType in data elements")
        }
        dataTypeCacheableRepository.deleteAll(dataTypes)
    }

    protected List<DataElement> getDataTypes(DataClass dataClass) {
        dataClass.dataElements.collect {childDE ->
            childDE.dataType = dataTypeCacheableRepository.readById(childDE.dataType.id)
        }
        dataClass.dataElements
    }
}