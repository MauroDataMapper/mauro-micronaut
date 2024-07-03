package uk.ac.ox.softeng.mauro.persistence.dataflow

import groovy.transform.CompileStatic
import io.micronaut.core.annotation.NonNull
import jakarta.inject.Inject
import jakarta.inject.Singleton
import uk.ac.ox.softeng.mauro.domain.dataflow.DataElementComponent
import uk.ac.ox.softeng.mauro.domain.model.AdministeredItem
import uk.ac.ox.softeng.mauro.persistence.cache.AdministeredItemCacheableRepository
import uk.ac.ox.softeng.mauro.persistence.model.AdministeredItemContentRepository

@CompileStatic
@Singleton
class DataElementComponentContentRepository extends AdministeredItemContentRepository {

    @Inject
    AdministeredItemCacheableRepository.DataElementComponentCacheableRepository dataElementComponentCacheableRepository

    @Inject
    DataElementComponentRepository dataElementComponentRepository

    @Override
    DataElementComponent readWithContentById(UUID id) {
        DataElementComponent dataElementComponent = dataElementComponentCacheableRepository.readById(id)
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
}