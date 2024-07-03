package uk.ac.ox.softeng.mauro.persistence.dataflow

import groovy.transform.CompileStatic
import jakarta.inject.Inject
import jakarta.inject.Singleton
import uk.ac.ox.softeng.mauro.domain.dataflow.DataFlow
import uk.ac.ox.softeng.mauro.persistence.cache.AdministeredItemCacheableRepository
import uk.ac.ox.softeng.mauro.persistence.model.AdministeredItemContentRepository

@CompileStatic
@Singleton
class DataFlowContentRepository extends AdministeredItemContentRepository {

    @Inject
    AdministeredItemCacheableRepository.DataFlowCacheableRepository dataFlowCacheableRepository

    @Override
    DataFlow readWithContentById(UUID id) {
        DataFlow dataFlow = dataFlowCacheableRepository.readById(id)
        dataFlow
    }

}
