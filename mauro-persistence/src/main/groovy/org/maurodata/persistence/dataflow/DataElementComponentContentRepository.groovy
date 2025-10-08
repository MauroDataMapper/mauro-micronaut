package org.maurodata.persistence.dataflow

import groovy.transform.CompileStatic
import io.micronaut.core.annotation.NonNull
import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.maurodata.domain.dataflow.DataElementComponent
import org.maurodata.domain.model.AdministeredItem
import org.maurodata.persistence.cache.AdministeredItemCacheableRepository
import org.maurodata.persistence.model.AdministeredItemContentRepository

@CompileStatic
@Singleton
class DataElementComponentContentRepository extends AdministeredItemContentRepository {

    @Inject
    AdministeredItemCacheableRepository.DataElementComponentCacheableRepository dataElementComponentRepository

    @Override
    DataElementComponent readWithContentById(UUID id) {
        DataElementComponent dataElementComponent = dataElementComponentRepository.readById(id)
        if (!dataElementComponent) return null
        dataElementComponent.sourceDataElements = dataElementComponentRepository.getSourceDataElements(dataElementComponent.id)
        dataElementComponent.targetDataElements = dataElementComponentRepository.getTargetDataElements(dataElementComponent.id)
        dataElementComponent
    }


    @Override
    AdministeredItem saveWithContent(@NonNull AdministeredItem administeredItem) {
        DataElementComponent saved = dataElementComponentRepository.save(administeredItem as DataElementComponent)
        saved.sourceDataElements.each {
            dataElementComponentRepository.addSourceDataElement(saved.id, it.id)
        }
        saved.targetDataElements.each {
            dataElementComponentRepository.addTargetDataElement(saved.id, it.id)
        }
        saveAllFacets(saved)
        saved
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

    @Override
    Boolean handles(Class clazz) {
        return clazz.simpleName == 'DataElementComponent'
    }
}