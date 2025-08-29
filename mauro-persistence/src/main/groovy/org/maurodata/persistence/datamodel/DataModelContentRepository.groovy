package org.maurodata.persistence.datamodel

import groovy.transform.CompileStatic
import io.micronaut.core.annotation.NonNull
import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.maurodata.domain.dataflow.DataClassComponent
import org.maurodata.domain.dataflow.DataElementComponent
import org.maurodata.domain.datamodel.DataClass
import org.maurodata.domain.datamodel.DataElement
import org.maurodata.domain.datamodel.DataModel
import org.maurodata.domain.datamodel.DataType
import org.maurodata.persistence.dataflow.DataFlowContentRepository
import org.maurodata.persistence.dataflow.DataFlowRepository
import org.maurodata.persistence.model.ModelContentRepository

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

    @Inject
    DataFlowRepository dataFlowRepository

    @Inject
    DataFlowContentRepository dataFlowContentRepository

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

        //dataFlows
        dataModel.sourceDataFlows = dataFlowRepository.findAllBySource(dataModel).collect {
            dataFlowContentRepository.readWithContentById(it.id)
        }
        dataModel.targetDataFlows = dataFlowRepository.findAllByTarget(dataModel).collect {
            dataFlowContentRepository.readWithContentById(it.id)
        }
        dataModel
    }


    @Override
    DataModel saveWithContent(@NonNull DataModel model) {
        DataModel saved = (DataModel) super.saveWithContent(model)
        Map<String, DataClass> dataClassByLabelLookup = [:]
        saved.allDataClasses.each {
            dataClassByLabelLookup.putIfAbsent(it.label, it)
        }

        Map<String, DataElement> dataElementByLabelLookup  = [:]
        saved.dataElements.each {
            dataElementByLabelLookup.putIfAbsent(it.label, it)
        }

        dataClassRepository.updateAll(saved.allDataClasses.findAll {dataClass ->
            dataClass.referenceTypes.collect {
                it.referenceClass = dataClass
            }
        })
        dataClassRepository.updateAll(saved.allDataClasses.findAll {it.parentDataClass})
        dataClassRepository.deleteExtensionRelationships(saved.allDataClasses.collect {it.id})
        saved.allDataClasses.each {dataClass ->
            dataClass.extendsDataClasses.each {extendedDataClass ->
                dataClassRepository.addDataClassExtensionRelationship(dataClass.id, extendedDataClass.id)
            }
        }
        saved.dataTypes = dataTypeRepository.updateAll(saved.dataTypes.findAll {it.isReferenceType()})

        saved = updateDataFlows(saved, dataClassByLabelLookup, dataElementByLabelLookup)

        saved.targetDataFlows.each {
            it.target = saved
            it.updateCreationProperties()
            dataFlowContentRepository.saveWithContent(it)
        }
        saved
    }

    @Override
    DataModel saveContentOnly(@NonNull DataModel model) {
        DataModel saved = (DataModel) super.saveContentOnly(model)
        if (saved.allDataClasses) {
            dataClassRepository.updateAll(saved.allDataClasses.findAll {it.parentDataClass})
        }
        if (model.dataElements) {
            dataElementRepository.updateAll(model.dataElements)
        }
        saved
    }

    private DataModel updateDataFlows(DataModel dataModel, Map<String, DataClass> dataClassDataByLabelLookup, Map<String, DataElement> dataElementByLabelLookup) {
        dataModel.sourceDataFlows.collect {
            it.dataClassComponents = updateDataClassComponentFlows(it.dataClassComponents, dataClassDataByLabelLookup, dataElementByLabelLookup)
        }

        dataModel.targetDataFlows.collect {
            it.dataClassComponents = updateDataClassComponentFlows(it.dataClassComponents, dataClassDataByLabelLookup, dataElementByLabelLookup)
        }
        dataModel
    }

    private List<DataClassComponent> updateDataClassComponentFlows(List<DataClassComponent> dataClassComponents, Map<String, DataClass> dataClassByLabelLookup,
                                                                  Map<String, DataElement> dataElementByLabelLookup) {
        dataClassComponents.collect {
            it.sourceDataClasses = updateDataClasses(it.sourceDataClasses, dataClassByLabelLookup)
            it.targetDataClasses = updateDataClasses(it.targetDataClasses, dataClassByLabelLookup)
            it.dataElementComponents = updateDataElementsInDataElementComponents(it.dataElementComponents, dataElementByLabelLookup)
        }
        dataClassComponents
    }

    private List<DataElementComponent> updateDataElementsInDataElementComponents(List<DataElementComponent> dataElementComponents, Map<String, DataElement> dataElementByLabelLookup) {
        dataElementComponents.collect {
            it.sourceDataElements = updateDataElements(it.sourceDataElements, dataElementByLabelLookup)
            it.targetDataElements = updateDataElements(it.targetDataElements, dataElementByLabelLookup)
        }
        dataElementComponents
    }

    private List<DataClass> updateDataClasses(List<DataClass> dataClasses, Map<String, DataClass> dataClassByLabelLookup) {
        dataClasses.collect {
            dataClassByLabelLookup.getOrDefault(it.label, it)
        }
    }

    private List<DataElement> updateDataElements(List<DataElement> dataElements, Map<String, DataElement> dataElementByLabelLookup) {
        dataElements.collect {
            dataElementByLabelLookup.getOrDefault(it.label, it)
        }
    }
}
