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

    @Inject
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
        if (!dataClass.parentDataClass) {
            dataClass.dataClasses = dataClassCacheableRepository.readAllByParentDataClass_Id(id)
            dataClass.dataClasses.collect {
                it.dataElements = dataElementCacheableRepository.readAllByDataClass_Id(it.id)
                it.dataElements = getDataTypes(it)
            }
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
        if ((administeredItem as DataClass).dataElements) {
            List<DataElement> dataElements = (administeredItem as DataClass).dataElements
            dataElements.each {
                if (it.dataType.isReferenceType()) {
                    deleteDanglingReferenceTypes(it.dataType.referenceClass)
                }
            }
            dataElementCacheableRepository.deleteAll(dataElements)
        }
        if ((administeredItem as DataClass).dataClasses) {
            List<DataClass> children = (administeredItem as DataClass).dataClasses
            children.each {
                deleteDanglingReferenceTypes(it)
            }
            dataClassCacheableRepository.deleteAll(children)
        }
        deleteDanglingReferenceTypes(administeredItem as DataClass)
        dataClassCacheableRepository.delete(administeredItem as DataClass)
    }

    void deleteDanglingReferenceTypes(DataClass dataClassToDelete) {
        List<DataType> dataTypes = dataTypeCacheableRepository.findAllByReferenceClass(dataClassToDelete).unique() as List<DataType>
        dataTypes.each {
            List<DataElement> dataElementReferenced = dataElementCacheableRepository.readAllByDataType(it as org.maurodata.domain.datamodel.DataType)
            List<DataElement> dataElementReferences = dataElementReferenced.findAll {dataElement -> dataElement.dataClass != dataClassToDelete}.collect()
            if (dataElementReferences.isEmpty()) {
                dataTypeCacheableRepository.delete(it as org.maurodata.domain.datamodel.DataType)
            } else {
                ErrorHandler.handleError(HttpStatus.UNPROCESSABLE_ENTITY, "Cannot delete Data Class has associations - check dataElements")
            }
        }
    }

    protected List<DataElement> getDataTypes(DataClass dataClass) {
        dataClass.dataElements.collect {childDE ->
            childDE.dataType = dataTypeCacheableRepository.readById(childDE.dataType.id)
        }
        dataClass.dataElements
    }
}