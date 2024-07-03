package uk.ac.ox.softeng.mauro.persistence.dataflow.dto

import groovy.transform.CompileStatic
import io.micronaut.core.annotation.Nullable
import io.micronaut.data.annotation.Join
import io.micronaut.data.annotation.Query
import io.micronaut.data.jdbc.annotation.JdbcRepository
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.repository.GenericRepository
import uk.ac.ox.softeng.mauro.domain.dataflow.DataClassComponent
import uk.ac.ox.softeng.mauro.domain.datamodel.DataElement

@CompileStatic
@JdbcRepository(dialect = Dialect.POSTGRES)
abstract class DataElementComponentDTORepository implements GenericRepository<DataElementComponentDTO, UUID> {


    @Join(value = 'catalogueUser', type = Join.Type.LEFT_FETCH)
    @Nullable
    abstract List<DataElementComponentDTO> findAllByDataClassComponent(DataClassComponent dataClassComponent)


    @Join(value = 'catalogueUser', type = Join.Type.LEFT_FETCH)
    @Nullable
    abstract DataElementComponentDTO findById(UUID id)

    /**
     * Add source dataElement to dataElementComponent
     * * @param id            dataElementComponentId
     * * @param dataElementId  dataElementId of source data Element
     * @returns: DataClassComponentDTO
     */
    @Query(''' insert into dataflow.data_element_component_target_data_element(data_element_component_id, data_element_id) values (:id, :dataElementId) ''')
    abstract DataElementComponentDTO addTargetDataElement(UUID id, UUID dataElementId)


    /**
     * Add target dataElement to dataElementComponent
     * * @param id            dataElementComponentId
     * * @param dataElementId  dataElementId of target data Element
     * @returns: DataClassComponentDTO
     */
    @Query(''' insert into dataflow.data_element_component_source_data_element(data_element_component_id, data_element_id) values (:id, :dataElementId) ''')
    abstract DataElementComponentDTO addSourceDataElement(UUID id, UUID dataElementId)

    @Query(''' select * from datamodel.data_element de where exists (select data_element_id from dataflow.data_element_component_source_data_element s
                where s.data_element_id = de.id and s.data_element_component_id = :id) ''')
    @Nullable
    abstract List<DataElement> getSourceDataElements(UUID id)

    @Query(''' select * from datamodel.data_element de where exists (select data_element_id from dataflow.data_element_component_target_data_element s
                where s.data_element_id = de.id and s.data_element_component_id = :id) ''')
    @Nullable
    abstract List<DataElement> getTargetDataElements(UUID id)

    @Query(''' delete from dataflow.data_element_component_source_data_element t where t.data_element_component_id = :id and t.data_element_id = :dataElementId ''')
    abstract Long removeSourceDataElement(UUID id, UUID dataElementId)

    @Query(''' delete from dataflow.data_element_component_target_data_element t where t.data_element_component_id = :id and t.data_element_id = :dataElementId ''')
    abstract Long removeTargetDataElement(UUID id, UUID dataElementId)

    @Query(''' delete from dataflow.data_element_component_source_data_element t where t.data_element_component_id = :id ''')
    abstract Long removeSourceDataElements(UUID id)

    @Query(''' delete from dataflow.data_element_component_target_data_element t where t.data_element_component_id = :id ''')
    abstract Long removeTargetDataElements(UUID id)
}
