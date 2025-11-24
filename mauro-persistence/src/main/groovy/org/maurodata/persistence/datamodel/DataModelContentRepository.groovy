package org.maurodata.persistence.datamodel

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import io.micronaut.core.annotation.NonNull
import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.maurodata.domain.datamodel.DataClass
import org.maurodata.domain.datamodel.DataModel
import org.maurodata.domain.datamodel.DataType
import org.maurodata.persistence.cache.AdministeredItemCacheableRepository
import org.maurodata.persistence.dataflow.DataFlowContentRepository
import org.maurodata.persistence.model.ModelContentRepository

@CompileStatic
@Singleton
@Slf4j
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
    AdministeredItemCacheableRepository.DataFlowCacheableRepository dataFlowCacheableRepository

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
        dataModel.sourceDataFlows = dataFlowCacheableRepository.findAllBySource(dataModel).collect {
            dataFlowContentRepository.readWithContentById(it.id)
        }
        dataModel.targetDataFlows = dataFlowCacheableRepository.findAllByTarget(dataModel).collect {
            dataFlowContentRepository.readWithContentById(it.id)
        }
        dataModel
    }


    @Override
    DataModel saveWithContent(@NonNull DataModel model) {
        DataModel saved = (DataModel) super.saveWithContent(model)
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
        saved.targetDataFlows.each {dataFlow ->
            dataFlow.target = saved
            dataFlow.updateCreationProperties()
            dataFlowContentRepository.saveWithContent(dataFlow)
        }
        saved
    }

    @Override
    DataModel saveContentOnly(@NonNull DataModel model) {
        DataModel saved = (DataModel) super.saveContentOnly(model)
        if(saved.allDataClasses) {
            dataClassRepository.updateAll(saved.allDataClasses.findAll { it.parentDataClass})
        }
        if(model.dataElements) {
            dataElementRepository.updateAll(model.dataElements)
        }
        saved
    }

    @Override
    Boolean handles(String domainType) {
        return dataModelRepository.handles(domainType)
    }

    @Override
    Boolean handles(Class clazz) {
        return dataModelRepository.handles(clazz)
    }
}
