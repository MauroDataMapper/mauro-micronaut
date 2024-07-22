package uk.ac.ox.softeng.mauro.persistence.dataflow

import groovy.transform.CompileStatic
import io.micronaut.core.annotation.NonNull
import jakarta.inject.Inject
import jakarta.inject.Singleton
import uk.ac.ox.softeng.mauro.domain.dataflow.DataFlow
import uk.ac.ox.softeng.mauro.domain.model.AdministeredItem
import uk.ac.ox.softeng.mauro.persistence.model.AdministeredItemContentRepository

@CompileStatic
@Singleton
class DataFlowContentRepository extends AdministeredItemContentRepository {

    @Inject
    DataFlowRepository dataFlowRepository
    @Inject
    DataClassComponentContentRepository dataClassComponentContentRepository

    @Override
    DataFlow readWithContentById(UUID id) {
        dataFlowRepository.readWithContentById(id)
    }

    @Override
    Long deleteWithContent(@NonNull AdministeredItem administeredItem) {
        DataFlow dataFlow = administeredItem as DataFlow
        if (dataFlow.dataClassComponents) {
            dataFlow.dataClassComponents.each { dataClassComponent ->
                dataClassComponentContentRepository.deleteWithContent(dataClassComponent as AdministeredItem)
            }
        }
        dataFlowRepository.delete(dataFlow)
    }
}
