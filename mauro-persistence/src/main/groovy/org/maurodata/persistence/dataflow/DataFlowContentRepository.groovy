package org.maurodata.persistence.dataflow

import groovy.transform.CompileStatic
import io.micronaut.core.annotation.NonNull
import jakarta.inject.Inject
import jakarta.inject.Singleton
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

    @Override
    DataFlow readWithContentById(UUID id) {
        dataFlowRepository.readWithContentById(id)
    }


    @Override
    DataFlow saveWithContent(AdministeredItem administeredItem) {
        List<Collection<AdministeredItem>> associations = administeredItem.getAllAssociations()

        DataFlow saved = dataFlowRepository.save(administeredItem as DataFlow)

        AdministeredItem savedAllContent = super.saveFacetsAndAssociations(saved, associations)
        savedAllContent as DataFlow
        saved.dataClassComponents.each{
            it.dataFlow = saved
            it.parent
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
        dataFlowRepository.delete(dataFlow)
    }
}
