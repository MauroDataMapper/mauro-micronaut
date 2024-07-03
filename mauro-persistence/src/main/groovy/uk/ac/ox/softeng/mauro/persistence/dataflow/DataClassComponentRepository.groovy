package uk.ac.ox.softeng.mauro.persistence.dataflow

import groovy.transform.CompileStatic
import groovy.transform.MapConstructor
import io.micronaut.core.annotation.Nullable
import io.micronaut.data.jdbc.annotation.JdbcRepository
import io.micronaut.data.model.query.builder.sql.Dialect
import jakarta.inject.Inject
import uk.ac.ox.softeng.mauro.domain.dataflow.DataClassComponent
import uk.ac.ox.softeng.mauro.domain.dataflow.DataFlow
import uk.ac.ox.softeng.mauro.domain.datamodel.DataClass
import uk.ac.ox.softeng.mauro.domain.model.AdministeredItem
import uk.ac.ox.softeng.mauro.persistence.dataflow.dto.DataClassComponentDTORepository
import uk.ac.ox.softeng.mauro.persistence.model.ModelItemRepository

@CompileStatic
@JdbcRepository(dialect = Dialect.POSTGRES)
@MapConstructor(includeSuperFields = true, includeSuperProperties = true, noArg = true)
abstract class DataClassComponentRepository implements ModelItemRepository<DataClassComponent> {

    @Inject
    DataClassComponentDTORepository dataClassComponentDTORepository

    @Nullable
    DataClassComponent findById(UUID id) {
        dataClassComponentDTORepository.findById(id) as DataClassComponent
    }


    @Nullable
    List<DataClassComponent> findAllByParent(AdministeredItem dataFlow) {
        findAllByDataFlow((DataFlow) dataFlow) as List<DataClassComponent>
    }

    @Nullable
    List<DataClassComponent> findAllByDataFlow(DataFlow dataFlow) {
        dataClassComponentDTORepository.findAllByDataFlowId((dataFlow as DataFlow).id) as List<DataClassComponent>
    }

    @Nullable
    List<DataClassComponent> readAllByParent(AdministeredItem parent) {
        readAllByDataFlow((DataFlow) parent)
    }

    DataClassComponent addSourceDataClass(DataClassComponent dataClassComponent, UUID dataClassId) {
        DataClassComponent persisted = dataClassComponentDTORepository.addSourceDataClass(dataClassComponent.id, dataClassId) as DataClassComponent
        persisted
    }

    DataClassComponent addTargetDataClass(DataClassComponent dataClassComponent, UUID dataClassId) {
        DataClassComponent persisted = dataClassComponentDTORepository.addTargetDataClass(dataClassComponent.id, dataClassId) as DataClassComponent
        persisted
    }

    Class getDomainClass() {
        DataClassComponent
    }

    Long removeSourceDataClass(DataClassComponent dataClassComponent, UUID dataClassId) {
        Long result = dataClassComponentDTORepository.removeSourceDataClass(dataClassComponent.id, dataClassId)
        result
    }

    Long removeTargetDataClass(DataClassComponent dataClassComponent, UUID dataClassId) {
        Long result = dataClassComponentDTORepository.removeTargetDataClass(dataClassComponent.id, dataClassId)
        result
    }

    abstract List<DataClassComponent> readAllByDataFlow(DataFlow dataFlow)


    List<DataClass> getDataClassesFromDataClassComponentToSourceDataClass(UUID id) {
        dataClassComponentDTORepository.getDataClassesFromDataClassComponentToSourceDataClass(id)
    }

    List<DataClass> getDataClassesFromDataClassComponentToTargetDataClass(UUID id) {
        dataClassComponentDTORepository.getDataClassesFromDataClassComponentToTargetDataClass(id)
    }

    Long removeSourceDataClasses(UUID id) {
        Long result = dataClassComponentDTORepository.removeSourceDataClasses(id)
        result
    }

    Long removeTargetDataClasses(UUID id) {
        Long result = dataClassComponentDTORepository.removeTargetDataClasses(id)
        result
    }
}

