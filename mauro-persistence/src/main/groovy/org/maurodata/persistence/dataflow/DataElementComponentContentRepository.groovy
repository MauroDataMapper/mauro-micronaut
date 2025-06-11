package org.maurodata.persistence.dataflow

import groovy.transform.CompileStatic
import io.micronaut.core.annotation.NonNull
import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.maurodata.domain.dataflow.DataElementComponent
import org.maurodata.domain.model.AdministeredItem
import org.maurodata.persistence.model.AdministeredItemContentRepository

@CompileStatic
@Singleton
class DataElementComponentContentRepository extends AdministeredItemContentRepository {

    @Inject
    DataElementComponentRepository dataElementComponentRepository

    @Override
    DataElementComponent readWithContentById(UUID id) {
        DataElementComponent dataElementComponent = dataElementComponentRepository.readById(id)
        if (!dataElementComponent) return null
        dataElementComponent.sourceDataElements = dataElementComponentRepository.getSourceDataElements(dataElementComponent.id)
        dataElementComponent.targetDataElements = dataElementComponentRepository.getTargetDataElements(dataElementComponent.id)
        dataElementComponent
    }

    // TODO methods here won't invalidate the cache
    @Override
    Long deleteWithContent(@NonNull AdministeredItem administeredItem) {
        if ((administeredItem as DataElementComponent).sourceDataElements) {
            dataElementComponentRepository.removeSourceDataElements(administeredItem.id)
        }
        if ((administeredItem as DataElementComponent).targetDataElements) {
            dataElementComponentRepository.removeTargetDataElements(administeredItem.id)
        }
        dataElementComponentRepository.delete(administeredItem as DataElementComponent)
    }

    void deleteAllSourceAndTargetDataElements(List<DataElementComponent> dataElementComponents) {
        dataElementComponents.each{
            dataElementComponentRepository.removeSourceDataElements(it.id)
            dataElementComponentRepository.removeTargetDataElements(it.id)
        }
    }

}