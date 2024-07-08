package uk.ac.ox.softeng.mauro.persistence.dataflow

import groovy.transform.CompileStatic
import io.micronaut.core.annotation.Nullable
import io.micronaut.data.annotation.Query
import io.micronaut.data.jdbc.annotation.JdbcRepository
import io.micronaut.data.model.query.builder.sql.Dialect
import jakarta.inject.Inject
import uk.ac.ox.softeng.mauro.domain.dataflow.DataFlow
import uk.ac.ox.softeng.mauro.domain.datamodel.DataModel
import uk.ac.ox.softeng.mauro.domain.model.AdministeredItem
import uk.ac.ox.softeng.mauro.persistence.dataflow.dto.DataFlowDTORepository
import uk.ac.ox.softeng.mauro.persistence.model.ModelItemRepository

@CompileStatic
@JdbcRepository(dialect = Dialect.POSTGRES)
abstract class DataFlowRepository implements ModelItemRepository<DataFlow> {

    @Inject
    DataFlowDTORepository dataFlowDTORepository

    @Override
    @Nullable
    DataFlow findById(UUID id) {
        dataFlowDTORepository.findById(id) as DataFlow
    }

    @Nullable
    List<DataFlow> findAllByTarget(DataModel dataModel) {
        dataFlowDTORepository.findAllByTarget(dataModel) as List<DataFlow>
    }
    @Nullable
    List<DataFlow> findAllBySource(DataModel dataModel) {
        dataFlowDTORepository.findAllBySource(dataModel) as List<DataFlow>
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
}
