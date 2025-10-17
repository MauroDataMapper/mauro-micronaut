package org.maurodata.persistence.dataflow

import groovy.transform.CompileStatic
import io.micronaut.core.annotation.NonNull
import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.maurodata.domain.dataflow.DataClassComponent
import org.maurodata.domain.dataflow.DataFlow
import org.maurodata.domain.model.AdministeredItem
import org.maurodata.persistence.cache.AdministeredItemCacheableRepository
import org.maurodata.persistence.model.AdministeredItemContentRepository

@CompileStatic
@Singleton
class DataFlowContentRepository extends AdministeredItemContentRepository {

    @Inject
    AdministeredItemCacheableRepository.DataFlowCacheableRepository dataFlowCacheableRepository
    @Inject
    DataClassComponentContentRepository dataClassComponentContentRepository

    @Inject
    AdministeredItemCacheableRepository.DataElementComponentCacheableRepository dataElementComponentCacheableRepository

    @Inject
    AdministeredItemCacheableRepository.DataClassComponentCacheableRepository dataClassComponentCacheableRepository

    @Override
    DataFlow readWithContentById(UUID id) {
        DataFlow dataFlow = dataFlowCacheableRepository.findById(id)
        List<DataClassComponent> dataClassComponents = dataClassComponentCacheableRepository.findAllByParent(dataFlow)

        dataClassComponents.each {
            it.dataElementComponents = dataElementComponentCacheableRepository.findAllByParent(it)
        }
        dataFlow.dataClassComponents = dataClassComponents
        dataFlow
    }

    @Override
    DataFlow saveWithContent(AdministeredItem administeredItem) {
        DataFlow saved = (DataFlow) super.saveWithContent(administeredItem)
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
