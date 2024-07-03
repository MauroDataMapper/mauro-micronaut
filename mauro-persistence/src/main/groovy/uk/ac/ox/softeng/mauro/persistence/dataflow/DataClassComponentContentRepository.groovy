package uk.ac.ox.softeng.mauro.persistence.dataflow

import groovy.transform.CompileStatic
import io.micronaut.core.annotation.NonNull
import jakarta.inject.Inject
import jakarta.inject.Singleton
import uk.ac.ox.softeng.mauro.domain.dataflow.DataClassComponent
import uk.ac.ox.softeng.mauro.domain.model.AdministeredItem
import uk.ac.ox.softeng.mauro.persistence.cache.AdministeredItemCacheableRepository
import uk.ac.ox.softeng.mauro.persistence.model.AdministeredItemContentRepository

@CompileStatic
@Singleton
class DataClassComponentContentRepository extends AdministeredItemContentRepository {

    @Inject
    AdministeredItemCacheableRepository.DataClassComponentCacheableRepository dataClassComponentCacheableRepository

    @Inject
    DataClassComponentRepository dataClassComponentRepository

    @Override
    DataClassComponent readWithContentById(UUID id) {
        DataClassComponent dataClassComponent = dataClassComponentCacheableRepository.readById(id)
        if (!dataClassComponent) return null
        dataClassComponent.sourceDataClasses = dataClassComponentRepository.getDataClassesFromDataClassComponentToSourceDataClass(dataClassComponent.id)
        dataClassComponent.targetDataClasses = dataClassComponentRepository.getDataClassesFromDataClassComponentToTargetDataClass( dataClassComponent.id)
        dataClassComponent
    }

    @Override
    Long deleteWithContent(@NonNull AdministeredItem administeredItem) {
        if ((administeredItem as DataClassComponent).sourceDataClasses) {
            dataClassComponentRepository.removeSourceDataClasses(administeredItem.id)
        }
        if ((administeredItem as DataClassComponent).targetDataClasses) {
            dataClassComponentRepository.removeTargetDataClasses(administeredItem.id)
        }
        dataClassComponentRepository.delete(administeredItem as DataClassComponent)
    }
}
