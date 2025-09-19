package org.maurodata.persistence.dataflow

import groovy.transform.CompileStatic
import io.micronaut.core.annotation.Nullable
import io.micronaut.data.jdbc.annotation.JdbcRepository
import io.micronaut.data.model.query.builder.sql.Dialect
import jakarta.inject.Inject
import org.maurodata.domain.dataflow.DataClassComponent
import org.maurodata.domain.dataflow.DataFlow
import org.maurodata.domain.datamodel.DataModel
import org.maurodata.domain.model.AdministeredItem
import org.maurodata.persistence.dataflow.dto.DataFlowDTORepository
import org.maurodata.persistence.model.ModelItemRepository

@CompileStatic
@JdbcRepository(dialect = Dialect.POSTGRES)
abstract class DataFlowRepository implements ModelItemRepository<DataFlow> {

    @Inject
    DataFlowDTORepository dataFlowDTORepository

    @Inject
    DataClassComponentRepository dataClassComponentRepository

    @Inject
    DataElementComponentRepository dataElementComponentRepository


    @Override
    @Nullable
    DataFlow findById(UUID id) {
        dataFlowDTORepository.findById(id) as DataFlow
    }

    @Nullable
    List<DataFlow> findAllByParentAndPathIdentifier(UUID item,String pathIdentifier) {
        dataFlowDTORepository.findAllByParentAndPathIdentifier(item,pathIdentifier)
    }

    @Nullable
    List<DataFlow> findAllByTarget(DataModel dataModel) {
        dataFlowDTORepository.findAllByTarget(dataModel) as List<DataFlow>
    }

    @Nullable
    List<DataFlow> findAllBySource(DataModel dataModel) {
        dataFlowDTORepository.findAllBySource(dataModel) as List<DataFlow>
    }


    @Nullable
    List<DataFlow> findAllByLabelContaining(String label){
        dataFlowDTORepository.findAllByLabelContaining(label)
    }
    @Override
    @Nullable
    List<DataFlow> readAllByParent(AdministeredItem parent) {
        findAllByTarget((DataModel) parent)
    }

    @Override
    Class getDomainClass() {
        DataFlow
    }

    DataFlow readWithContentById(UUID id) {
        DataFlow dataFlow = dataFlowDTORepository.findById(id)
        List<DataClassComponent> dataClassComponents = dataClassComponentRepository.findAllByParent(dataFlow)
        dataClassComponents.each {
            it.dataElementComponents = dataElementComponentRepository.findAllByParent(it)
        }
        dataFlow.dataClassComponents = dataClassComponents
        dataFlow
    }

    @Override
    @Nullable
    DataFlow readById(UUID id){
       //return dataFlow with full source and target DataModels
       findById(id)
    }

    Boolean handlesPathPrefix(final String pathPrefix) {
        'df'.equalsIgnoreCase(pathPrefix)
    }
}
