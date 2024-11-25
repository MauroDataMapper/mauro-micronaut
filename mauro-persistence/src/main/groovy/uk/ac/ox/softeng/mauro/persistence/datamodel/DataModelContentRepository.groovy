package uk.ac.ox.softeng.mauro.persistence.datamodel

import uk.ac.ox.softeng.mauro.domain.datamodel.DataClass
import uk.ac.ox.softeng.mauro.domain.datamodel.DataModel
import uk.ac.ox.softeng.mauro.domain.datamodel.DataType
import uk.ac.ox.softeng.mauro.persistence.cache.AdministeredItemCacheableRepository
import uk.ac.ox.softeng.mauro.persistence.model.ModelContentRepository

import groovy.transform.CompileStatic
import io.micronaut.core.annotation.NonNull
import jakarta.inject.Inject
import jakarta.inject.Singleton
import uk.ac.ox.softeng.mauro.domain.datamodel.DataClass
import uk.ac.ox.softeng.mauro.domain.datamodel.DataModel
import uk.ac.ox.softeng.mauro.persistence.model.ModelContentRepository

@CompileStatic
@Singleton
class DataModelContentRepository extends ModelContentRepository<DataModel> {

    @Inject
    DataModelRepository dataModelRepository

    @Inject
    DataClassRepository dataClassRepository

    @Inject
    DataTypeRepository dataTypeRepository

    @Inject
    DataElementRepository dataElementRepository

    @Inject
    EnumerationValueRepository enumerationValueRepository

    @Override
    DataModel findWithContentById(UUID id) {
        DataModel dataModel = dataModelRepository.findById(id)
        dataModel.allDataClasses = dataClassRepository.findAllByParent(dataModel) as Set
        dataModel.dataClasses = dataModel.allDataClasses.findAll{!it.parentDataClass }.sort {it.order}

        Map<UUID, DataClass> dataClassMap = dataModel.allDataClasses.collectEntries {[it.id, it]}

        dataModel.dataTypes = dataTypeRepository.findAllByParent(dataModel)

        Map<UUID, DataType> dataTypeMap = dataModel.dataTypes.collectEntries {[it.id, it]}
        if(dataModel.dataTypes) {
            dataModel.enumerationValues = enumerationValueRepository.readAllByEnumerationTypeIn(dataModel.dataTypes)
            dataModel.enumerationValues.each {enumerationValue ->
                enumerationValue.enumerationType = dataTypeMap[enumerationValue.enumerationType.id]
                enumerationValue.enumerationType.enumerationValues.add(enumerationValue)
                enumerationValue.dataModel = dataModel
            }
        }

        dataModel.dataClasses.each {dataClass ->
            dataClass.dataClasses = dataModel.allDataClasses.findAll{it.parentDataClass?.id == dataClass.id }.sort {it.order}
        }
        if (!dataModel.allDataClasses.isEmpty()) {
            dataModel.dataElements = dataElementRepository.findAllByDataClassIn(dataModel.allDataClasses)
        }
        dataModel.dataElements.each {dataElement ->
            dataElement.dataClass = dataClassMap[dataElement.dataClass.id]
            dataElement.dataClass.dataElements.add(dataElement)
            dataElement.dataType = dataTypeMap[dataElement.dataType.id]
        }

        dataModel
    }


    @Override
    DataModel saveWithContent(@NonNull DataModel model) {
        DataModel saved = (DataModel) super.saveWithContent(model)
        dataClassRepository.updateAll(saved.allDataClasses.findAll { it.parentDataClass})
        saved
    }
}
