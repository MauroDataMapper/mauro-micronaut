package uk.ac.ox.softeng.mauro.persistence.dataflow.dto

import groovy.transform.CompileStatic
import io.micronaut.core.annotation.NonNull
import io.micronaut.core.annotation.Nullable
import io.micronaut.data.annotation.Join
import io.micronaut.data.annotation.Query
import io.micronaut.data.jdbc.annotation.JdbcRepository
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.repository.GenericRepository
import uk.ac.ox.softeng.mauro.domain.dataflow.DataFlow
import uk.ac.ox.softeng.mauro.domain.datamodel.DataClass

@CompileStatic
@JdbcRepository(dialect = Dialect.POSTGRES)
abstract class DataClassComponentDTORepository implements GenericRepository<DataClassComponentDTO, UUID> {

    @Join(value = 'catalogueUser', type = Join.Type.LEFT_FETCH)
    @Nullable
    abstract DataClassComponentDTO findById(UUID id)

    @Join(value = 'catalogueUser', type = Join.Type.LEFT_FETCH)
    @Nullable
    abstract List<DataClassComponentDTO> findAllByDataFlowId(UUID uuid)

    /**
     * Add sourcedataClass to dataClassComponent
     * * @param id            dataClassComponentId
     * * @param dataClassId  dataClassId of source data class
     * @returns: DataClassComponentDTO
     */
    @Query(''' insert into dataflow.data_class_component_source_data_class(data_class_component_id, data_class_id) values (:id, :dataClassId) ''')
    abstract DataClassComponentDTO addSourceDataClass(@NonNull UUID id, @NonNull UUID dataClassId)


    /**
     * Add target dataClass  to dataClassComponent
     * * @param id            dataClassComponentId
     * * @param dataClassId  dataClassId of target data class
     * @returns: DataClassComponentDTO
     */
    @Query(''' insert into dataflow.data_class_component_target_data_class(data_class_component_id, data_class_id) values (:id, :dataClassId) ''')
    abstract DataClassComponentDTO addTargetDataClass(@NonNull UUID id, @NonNull UUID dataClassId)

    /**
     * Remove  dataClass from data_class_component_target_data_class
     * * @param id           dataClassComponentId
     * * @param dataClassId  dataClassId
     * @returns: DataClassComponentDTO
     */
    @Query(''' delete from dataflow.data_class_component_target_data_class dcctdc where dcctdc.data_class_component_id = :id and dcctdc.data_class_id = :dataClassId ''')
    abstract Long removeTargetDataClass(@NonNull UUID id, @NonNull UUID dataClassId)

    /**
     * Remove dataClass from data_class_component_source_data_class
     * * @param id           dataClassComponentId
     * * @param dataClassId  dataClassId
     * @returns: DataClassComponentDTO
     */
    @Query(''' delete from dataflow.data_class_component_source_data_class dccsdc where dccsdc.data_class_component_id = :id and dccsdc.data_class_id = :dataClassId ''')
    abstract Long removeSourceDataClass(@NonNull UUID id, @NonNull UUID dataClassId)

    @Query(''' select * from datamodel.data_class dc where exists (select data_class_id from dataflow.data_class_component_source_data_class s
                where s.data_class_id = dc.id and s.data_class_component_id = :id) ''')
    @Nullable
    abstract List<DataClass> getDataClassesFromDataClassComponentToSourceDataClass(@NonNull UUID id)

    @Query(''' select * from datamodel.data_class dc where exists (select data_class_id from dataflow.data_class_component_target_data_class t
                where t.data_class_id = dc.id and t.data_class_component_id = :id) ''')
    @Nullable
    abstract List<DataClass> getDataClassesFromDataClassComponentToTargetDataClass(@NonNull UUID id)

    @Query(''' delete from dataflow.data_class_component_source_data_class s where s.data_class_component_id = :id ''')
    abstract Long removeSourceDataClasses(UUID id)

    @Query(''' delete from dataflow.data_class_component_target_data_class t where t.data_class_component_id = :id ''')
    abstract Long removeTargetDataClasses(UUID id)
}
