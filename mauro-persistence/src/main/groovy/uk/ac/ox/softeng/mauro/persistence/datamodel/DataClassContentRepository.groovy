package uk.ac.ox.softeng.mauro.persistence.datamodel

import uk.ac.ox.softeng.mauro.domain.datamodel.DataClass
import uk.ac.ox.softeng.mauro.domain.datamodel.DataElement
import uk.ac.ox.softeng.mauro.domain.model.AdministeredItem
import uk.ac.ox.softeng.mauro.persistence.cache.AdministeredItemCacheableRepository
import uk.ac.ox.softeng.mauro.persistence.cache.AdministeredItemCacheableRepository.DataClassCacheableRepository
import uk.ac.ox.softeng.mauro.persistence.model.AdministeredItemContentRepository

import groovy.transform.CompileStatic
import io.micronaut.core.annotation.NonNull
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


    protected List<DataElement> getDataTypes(DataClass dataClass) {
        dataClass.dataElements.collect {childDE ->
            childDE.dataType = dataTypeCacheableRepository.readById(childDE.dataType.id)
        }
        dataClass.dataElements
    }
}