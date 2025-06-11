package org.maurodata.persistence.datamodel

import org.maurodata.domain.datamodel.DataClass
import org.maurodata.domain.model.AdministeredItem
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

    @Inject
    DataClassRepository dataClassRepository

    @Override
    DataClass readWithContentById(UUID id) {
        DataClass dataClass = dataClassCacheableRepository.findById(id)
        if (!dataClass.parentDataClass) {
            dataClass.dataClasses = dataClassCacheableRepository.readAllByParentDataClass_Id( id)
        }
        //todo other dataclass associations
        //eg dataElements

        dataClass.extendsDataClasses = dataClassRepository.getDataClassExtensionRelationships(id)
        dataClass.extendsDataClasses.each {it.extendedBy.add(dataClass)}

        dataClass
    }

    @Override
    DataClass saveWithContent(AdministeredItem administeredItem) {
        DataClass saved = (DataClass) super.saveWithContent(administeredItem)
        DataClass dataClass = (DataClass) administeredItem
        Map<String, DataClass> dataClassMap = [:]
        if(dataClass.extendsDataClasses.find{it.label && !it.id}) {
            dataClassRepository.findAllByDataModel(dataClass.dataModel).each {
                dataClassMap[it.label] = it
            }
        }
        dataClass.extendsDataClasses.each {extendedDataClass ->
            if(extendedDataClass.id) {
                dataClassRepository.addDataClassExtensionRelationship(saved.id, extendedDataClass.id)
            }
            else if(extendedDataClass.label) {
                if(dataClassMap[extendedDataClass.label]) {
                    UUID extendedDataClassId =  dataClassMap[extendedDataClass.label].id
                    dataClassRepository.addDataClassExtensionRelationship(saved.id, extendedDataClassId)
                }
            }
        }
        saved
    }
}
