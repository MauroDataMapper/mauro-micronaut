package org.maurodata.persistence.dataflow

import groovy.transform.CompileStatic
import io.micronaut.core.annotation.NonNull
import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.maurodata.domain.dataflow.DataClassComponent
import org.maurodata.domain.dataflow.DataElementComponent
import org.maurodata.domain.dataflow.DataFlow
import org.maurodata.domain.model.AdministeredItem
import org.maurodata.persistence.model.AdministeredItemContentRepository

@CompileStatic
@Singleton
class DataFlowContentRepository extends AdministeredItemContentRepository {

    @Inject
    DataFlowRepository dataFlowRepository
    @Inject
    DataClassComponentContentRepository dataClassComponentContentRepository
    @Inject
    DataClassComponentRepository dataClassComponentRepository

    @Inject
    DataElementComponentRepository dataElementComponentRepository

    @Override
    DataFlow readWithContentById(UUID id) {
        DataFlow dataFlow = dataFlowRepository.findById(id)
        List<DataClassComponent> dataClassComponents = dataClassComponentRepository.findAllByParent(dataFlow)

        dataClassComponents.each {
            it.dataElementComponents =   dataElementComponentRepository.findAllByParent(it)
        }
        dataFlow.dataClassComponents = dataClassComponents
        dataFlow
    }

    @Override
    DataFlow saveWithContent(AdministeredItem administeredItem) {
        DataFlow saved = dataFlowRepository.save(administeredItem as DataFlow)
        saveAllFacets(saved)
        saved.dataClassComponents.each {
            it.updateCreationProperties()
            it.dataFlow = saved
            it.parent = saved
            dataClassComponentContentRepository.saveWithContent(it)
        }
        saved
    }

    @Override
    Long deleteWithContent(@NonNull AdministeredItem administeredItem) {
        DataFlow dataFlow = administeredItem as DataFlow
        if (dataFlow.dataClassComponents) {
            dataFlow.dataClassComponents.each { dataClassComponent ->
                dataClassComponentContentRepository.deleteWithContent(dataClassComponent as AdministeredItem)
            }
        }
        super.administeredItemRepository.delete(dataFlow)
    }

    @Override
    Boolean handles(Class clazz) {
        return clazz.simpleName == 'DataFlow'
    }

}
