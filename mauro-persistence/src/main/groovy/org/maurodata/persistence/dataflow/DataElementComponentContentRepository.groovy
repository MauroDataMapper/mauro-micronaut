package org.maurodata.persistence.dataflow

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import io.micronaut.core.annotation.NonNull
import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.maurodata.domain.dataflow.DataElementComponent
import org.maurodata.domain.model.AdministeredItem
import org.maurodata.persistence.cache.AdministeredItemCacheableRepository
import org.maurodata.persistence.model.AdministeredItemContentRepository

@CompileStatic
@Slf4j
@Singleton
class DataElementComponentContentRepository extends AdministeredItemContentRepository {

    @Inject
    AdministeredItemCacheableRepository.DataElementComponentCacheableRepository dataElementComponentCacheableRepository

    @Override
    DataElementComponent readWithContentById(UUID id) {
        DataElementComponent dataElementComponent = dataElementComponentCacheableRepository.readById(id)
        if (!dataElementComponent) return null
        dataElementComponent.sourceDataElements = dataElementComponentCacheableRepository.getSourceDataElements(dataElementComponent.id)
        dataElementComponent.targetDataElements = dataElementComponentCacheableRepository.getTargetDataElements(dataElementComponent.id)
        dataElementComponent
    }


    @Override
    AdministeredItem saveWithContent(@NonNull AdministeredItem administeredItem) {
        DataElementComponent saved = dataElementComponentCacheableRepository.save(administeredItem as DataElementComponent)
        saveAllFacets(saved)
        saved
    }

    @Override
    Long deleteWithContent(@NonNull AdministeredItem administeredItem) {
        if ((administeredItem as DataElementComponent).sourceDataElements) {
            dataElementComponentCacheableRepository.removeSourceDataElements(administeredItem.id)
        }
        if ((administeredItem as DataElementComponent).targetDataElements) {
            dataElementComponentCacheableRepository.removeTargetDataElements(administeredItem.id)
        }
        dataElementComponentCacheableRepository.delete(administeredItem as DataElementComponent)
    }

    void deleteAllSourceAndTargetDataElements(List<DataElementComponent> dataElementComponents) {
        dataElementComponents.each{
            dataElementComponentCacheableRepository.removeSourceDataElements(it.id)
            dataElementComponentCacheableRepository.removeTargetDataElements(it.id)
        }
    }

    @Override
    Boolean handles(Class clazz) {
        return clazz.simpleName == 'DataElementComponent'
    }
}
