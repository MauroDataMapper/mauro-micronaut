package uk.ac.ox.softeng.mauro.persistence.datamodel

import uk.ac.ox.softeng.mauro.ErrorHandler
import uk.ac.ox.softeng.mauro.domain.datamodel.DataClass
import uk.ac.ox.softeng.mauro.domain.datamodel.DataElement
import uk.ac.ox.softeng.mauro.domain.datamodel.DataType
import uk.ac.ox.softeng.mauro.domain.model.AdministeredItem
import uk.ac.ox.softeng.mauro.persistence.cache.AdministeredItemCacheableRepository
import uk.ac.ox.softeng.mauro.persistence.cache.AdministeredItemCacheableRepository.DataClassCacheableRepository
import uk.ac.ox.softeng.mauro.persistence.model.AdministeredItemContentRepository

import groovy.transform.CompileStatic
import io.micronaut.core.annotation.NonNull
import io.micronaut.http.HttpStatus
import jakarta.inject.Inject
import jakarta.inject.Singleton

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
            } else if (extendedDataClass.label) {
                if (dataClassMap[extendedDataClass.label]) {
                    UUID extendedDataClassId = dataClassMap[extendedDataClass.label].id
                    dataClassRepository.addDataClassExtensionRelationship(saved.id, extendedDataClassId)
                }
            }
        }
        saved
    }

    @Override
    Long deleteWithContent(@NonNull AdministeredItem administeredItem) {
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
        List<DataType> dataTypes = dataTypeCacheableRepository.findAllByReferenceClass(dataClassToDelete).unique()
        dataTypes.each {
            List<DataElement> dataElementReferenced = dataElementCacheableRepository.readAllByDataType(it)
            List<DataElement> dataElementReferences = dataElementReferenced.findAll {dataElement -> dataElement.dataClass != dataClassToDelete}.collect()
            if (dataElementReferences.isEmpty()) {
                dataTypeCacheableRepository.delete(it)
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