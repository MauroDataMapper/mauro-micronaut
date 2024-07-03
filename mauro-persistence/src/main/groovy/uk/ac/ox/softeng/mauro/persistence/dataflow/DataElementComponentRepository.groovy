package uk.ac.ox.softeng.mauro.persistence.dataflow

import groovy.transform.CompileStatic
import groovy.transform.MapConstructor
import io.micronaut.core.annotation.Nullable
import io.micronaut.data.jdbc.annotation.JdbcRepository
import io.micronaut.data.model.query.builder.sql.Dialect
import jakarta.inject.Inject
import uk.ac.ox.softeng.mauro.domain.dataflow.DataClassComponent
import uk.ac.ox.softeng.mauro.domain.dataflow.DataElementComponent
import uk.ac.ox.softeng.mauro.domain.datamodel.DataElement
import uk.ac.ox.softeng.mauro.domain.model.AdministeredItem
import uk.ac.ox.softeng.mauro.persistence.dataflow.dto.DataElementComponentDTORepository
import uk.ac.ox.softeng.mauro.persistence.model.ModelItemRepository

@CompileStatic
@JdbcRepository(dialect = Dialect.POSTGRES)
@MapConstructor(includeSuperFields = true, includeSuperProperties = true, noArg = true)
abstract class DataElementComponentRepository implements ModelItemRepository<DataElementComponent> {

    @Inject
    DataElementComponentDTORepository dataElementComponentDTORepository
  
    @Override
    @Nullable
    DataElementComponent findById(UUID id) {
        dataElementComponentDTORepository.findById(id) as DataElementComponent
    }

    @Override
    @Nullable
    List<DataElementComponent> findAllByParent(AdministeredItem dataClassComponent) {
        findAllByDataClassComponent((DataClassComponent) dataClassComponent) as List<DataElementComponent>
    }

    @Nullable
    List<DataElementComponent> findAllByDataClassComponent(DataClassComponent dataClassComponent) {
        dataElementComponentDTORepository.findAllByDataClassComponent(dataClassComponent as DataClassComponent) as List<DataElementComponent>
    }


    @Nullable
    List<DataElementComponent> readAllByParent(AdministeredItem parent) {
        readAllByDataClassComponent((DataClassComponent) parent)
    }

    @Override
    Class getDomainClass() {
        DataElementComponent
    }

    abstract List<DataElementComponent> readAllByDataClassComponent(DataClassComponent dataClassComponent)

    DataElementComponent addTargetDataElement(DataElementComponent dataElementComponent, UUID dataElementId){
        dataElementComponentDTORepository.addTargetDataElement(dataElementComponent.id, dataElementId) as DataElementComponent
    }

    DataElementComponent addSourceDataElement(DataElementComponent dataElementComponent, UUID dataElementId){
        dataElementComponentDTORepository.addSourceDataElement(dataElementComponent.id, dataElementId)
    }

    List<DataElement> getSourceDataElements(UUID id) {
        dataElementComponentDTORepository.getSourceDataElements(id)
    }

    List<DataElement> getTargetDataElements(UUID id) {
        dataElementComponentDTORepository.getTargetDataElements(id)
    }

    Long removeSourceDataElement(DataElementComponent dataElementComponent, UUID dataElementId) {
        Long result = dataElementComponentDTORepository.removeSourceDataElement(dataElementComponent.id, dataElementId)
        result
    }

    Long removeTargetDataElement(DataElementComponent dataElementComponent, UUID dataElementId) {
        Long result = dataElementComponentDTORepository.removeTargetDataElement(dataElementComponent.id, dataElementId)
        result
    }

    Long removeSourceDataElements(UUID id){
        Long result = dataElementComponentDTORepository.removeSourceDataElements(id)
        result
    }

    Long removeTargetDataElements(UUID id){
        Long result = dataElementComponentDTORepository.removeTargetDataElements(id)
        result
    }
}
