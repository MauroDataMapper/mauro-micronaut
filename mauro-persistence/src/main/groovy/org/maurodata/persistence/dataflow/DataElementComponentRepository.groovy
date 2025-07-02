package org.maurodata.persistence.dataflow

import groovy.transform.CompileStatic
import groovy.transform.MapConstructor
import io.micronaut.core.annotation.NonNull
import io.micronaut.core.annotation.Nullable
import io.micronaut.data.annotation.Query
import io.micronaut.data.jdbc.annotation.JdbcRepository
import io.micronaut.data.model.query.builder.sql.Dialect
import jakarta.inject.Inject
import org.maurodata.domain.dataflow.DataClassComponent
import org.maurodata.domain.dataflow.DataElementComponent
import org.maurodata.domain.datamodel.DataElement
import org.maurodata.domain.model.AdministeredItem
import org.maurodata.persistence.dataflow.dto.DataElementComponentDTORepository
import org.maurodata.persistence.model.ModelItemRepository

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
    List<DataElementComponent> findAllByParentAndPathIdentifier(UUID item,String pathIdentifier) {
        dataElementComponentDTORepository.findAllByParentAndPathIdentifier(item,pathIdentifier)
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

    /**
     * Add source dataElement to dataElementComponent
     * * @param id            dataElementComponentId
     * * @param dataElementId  dataElementId of source data Element
     * @returns: DataClassComponentDTO
     */
    @Query(''' insert into dataflow.data_element_component_target_data_element(data_element_component_id, data_element_id) values (:id, :dataElementId) ''')
    abstract DataElementComponent addTargetDataElement(@NonNull UUID id, @NonNull UUID dataElementId)


    /**
     * Add target dataElement to dataElementComponent
     * * @param id            dataElementComponentId
     * * @param dataElementId  dataElementId of target data Element
     * @returns: DataClassComponentDTO
     */
    @Query(''' insert into dataflow.data_element_component_source_data_element(data_element_component_id, data_element_id) values (:id, :dataElementId) ''')
    abstract DataElementComponent addSourceDataElement(@NonNull UUID id, UUID dataElementId)

    @Query(''' select * from datamodel.data_element de where exists (select data_element_id from dataflow.data_element_component_source_data_element s
                where s.data_element_id = de.id and s.data_element_component_id = :id) ''')
    abstract List<DataElement> getSourceDataElements(UUID id)

    @Query(''' select * from datamodel.data_element de where exists (select data_element_id from dataflow.data_element_component_target_data_element s
                where s.data_element_id = de.id and s.data_element_component_id = :id) ''')
    abstract List<DataElement> getTargetDataElements(UUID id)

    @Query(''' delete from dataflow.data_element_component_source_data_element t where t.data_element_component_id = :id and t.data_element_id = :dataElementId ''')
    abstract Long removeSourceDataElement(@NonNull UUID id, UUID dataElementId)

    @Query(''' delete from dataflow.data_element_component_target_data_element t where t.data_element_component_id = :id and t.data_element_id = :dataElementId ''')
    abstract Long removeTargetDataElement(@NonNull UUID id, UUID dataElementId)

    @Query(''' delete from dataflow.data_element_component_source_data_element t where t.data_element_component_id = :id ''')
    abstract Long removeSourceDataElements(UUID id)

    @Query(''' delete from dataflow.data_element_component_target_data_element t where t.data_element_component_id = :id ''')
    abstract Long removeTargetDataElements(UUID id)

    Boolean handlesPathPrefix(final String pathPrefix) {
        'dec'.equalsIgnoreCase(pathPrefix)
    }
}
