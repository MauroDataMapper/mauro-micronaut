package org.maurodata.persistence.dataflow

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import io.micronaut.core.annotation.NonNull
import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.maurodata.domain.dataflow.DataClassComponent
import org.maurodata.domain.dataflow.DataElementComponent
import org.maurodata.domain.model.AdministeredItem
import org.maurodata.persistence.cache.AdministeredItemCacheableRepository
import org.maurodata.persistence.model.AdministeredItemContentRepository

@CompileStatic
@Slf4j
@Singleton
class DataClassComponentContentRepository extends AdministeredItemContentRepository {

    @Inject
    AdministeredItemCacheableRepository.DataClassComponentCacheableRepository dataClassComponentCacheableRepository

    @Inject
    DataElementComponentContentRepository dataElementComponentContentRepository

    @Inject
    AdministeredItemCacheableRepository.DataElementComponentCacheableRepository dataElementComponentCacheableRepository


    @Override
    DataClassComponent readWithContentById(UUID id) {
        DataClassComponent dataClassComponent = dataClassComponentCacheableRepository.findById(id)
        if (!dataClassComponent) return null
        dataClassComponent.sourceDataClasses = dataClassComponentCacheableRepository.findAllSourceDataClasses(dataClassComponent.id)
        dataClassComponent.targetDataClasses = dataClassComponentCacheableRepository.findAllTargetDataClasses(dataClassComponent.id)
        dataClassComponent.dataElementComponents = dataElementComponentCacheableRepository.findAllByParent(dataClassComponent)
        dataClassComponent
    }

    @Override
    Long deleteWithContent(@NonNull AdministeredItem administeredItem) {
        if ((administeredItem as DataClassComponent).dataElementComponents) {
            dataElementComponentContentRepository.deleteAllSourceAndTargetDataElements((administeredItem as DataClassComponent).dataElementComponents)
            dataElementComponentCacheableRepository.deleteAll( (administeredItem as DataClassComponent).dataElementComponents)
        }
        if ((administeredItem as DataClassComponent).sourceDataClasses) {
            dataClassComponentCacheableRepository.removeSourceDataClasses(administeredItem.id)
        }
        if ((administeredItem as DataClassComponent).targetDataClasses) {
            dataClassComponentCacheableRepository.removeTargetDataClasses(administeredItem.id)
        }
        dataClassComponentCacheableRepository.delete(administeredItem as DataClassComponent)
    }

    @Override
    AdministeredItem saveWithContent(@NonNull AdministeredItem administeredItem) {
        DataClassComponent saved = (DataClassComponent) super.saveWithContent(administeredItem)
        saved.dataElementComponents.each {
            it.updateCreationProperties()
            it.dataClassComponent = saved
            it.parent = saved
            (DataElementComponent) super.saveWithContent(it)
        }
        saveAllFacets(saved)
        saved
    }

    @Override
    Boolean handles(Class clazz) {
        return clazz.simpleName == 'DataClassComponent'
    }
}
