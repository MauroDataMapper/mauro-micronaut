package org.maurodata.persistence.dataflow

import groovy.transform.CompileStatic
import io.micronaut.core.annotation.NonNull
import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.maurodata.domain.dataflow.DataClassComponent
import org.maurodata.domain.model.AdministeredItem
import org.maurodata.persistence.model.AdministeredItemContentRepository

@CompileStatic
@Singleton
class DataClassComponentContentRepository extends AdministeredItemContentRepository {

    @Inject
    DataClassComponentRepository dataClassComponentRepository

    @Inject
    DataElementComponentContentRepository dataElementComponentContentRepository

    @Inject
    DataElementComponentRepository dataElementComponentRepository

    @Override
    DataClassComponent readWithContentById(UUID id) {
        DataClassComponent dataClassComponent = dataClassComponentRepository.findById(id)
        if (!dataClassComponent) return null
        dataClassComponent.sourceDataClasses = dataClassComponentRepository.findAllSourceDataClasses(dataClassComponent.id)
        dataClassComponent.targetDataClasses = dataClassComponentRepository.findAllTargetDataClasses(dataClassComponent.id)
        dataClassComponent.dataElementComponents = dataElementComponentRepository.findAllByParent(dataClassComponent)
        dataClassComponent
    }

    @Override
    Long deleteWithContent(@NonNull AdministeredItem administeredItem) {
        if ((administeredItem as DataClassComponent).dataElementComponents) {
            dataElementComponentContentRepository.deleteAllSourceAndTargetDataElements((administeredItem as DataClassComponent).dataElementComponents)
            dataElementComponentRepository.deleteAll( (administeredItem as DataClassComponent).dataElementComponents)
        }
        if ((administeredItem as DataClassComponent).sourceDataClasses) {
            dataClassComponentRepository.removeSourceDataClasses(administeredItem.id)
        }
        if ((administeredItem as DataClassComponent).targetDataClasses) {
            dataClassComponentRepository.removeTargetDataClasses(administeredItem.id)
        }
        dataClassComponentRepository.delete(administeredItem as DataClassComponent)
    }
}
