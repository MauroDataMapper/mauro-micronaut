package uk.ac.ox.softeng.mauro.persistence.dataflow

import groovy.transform.CompileStatic
import groovy.transform.MapConstructor
import io.micronaut.core.annotation.NonNull
import io.micronaut.core.annotation.Nullable
import io.micronaut.data.annotation.Query
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

    @Override
    @Nullable
    DataClassComponent findWithContentById(@NonNull UUID id, @NonNull AdministeredItem parent) {
        DataClassComponent dataClassComponent = findById(id)
        dataClassComponent.parent = parent
        dataClassComponent
    }

    /**
     * Add sourcedataClass to dataClassComponent
     * * @param id            dataClassComponentId
     * * @param dataClassId  dataClassId of source data class
     * @returns: DataClassComponentDTO
     */
    @Query(''' insert into dataflow.data_class_component_source_data_class(data_class_component_id, data_class_id) values (:id, :dataClassId) ''')
    abstract DataClassComponent addSourceDataClass(@NonNull UUID id, @NonNull UUID dataClassId)

    /**
     * Add target dataClass  to dataClassComponent
     * * @param id            dataClassComponentId
     * * @param dataClassId  dataClassId of target data class
     * @returns: DataClassComponentDTO
     */
    @Query(''' insert into dataflow.data_class_component_target_data_class(data_class_component_id, data_class_id) values (:id, :dataClassId) ''')
    abstract DataClassComponent addTargetDataClass(@NonNull UUID id, UUID dataClassId)


    Class getDomainClass() {
        DataClassComponent
    }

    /**
     * Remove dataClass from data_class_component_source_data_class
     * * @param id           dataClassComponentId
     * * @param dataClassId  dataClassId
     * @returns: DataClassComponentDTO
     */
    @Query(''' delete from dataflow.data_class_component_source_data_class dccsdc where dccsdc.data_class_component_id = :id and dccsdc.data_class_id = :dataClassId ''')
    abstract Long removeSourceDataClass(@NonNull UUID id, UUID dataClassId)


    /**
     * Remove  dataClass from data_class_component_target_data_class
     * * @param id           dataClassComponentId
     * * @param dataClassId  dataClassId
     * @returns: DataClassComponentDTO
     */
    @Query(''' delete from dataflow.data_class_component_target_data_class dcctdc where dcctdc.data_class_component_id = :id and dcctdc.data_class_id = :dataClassId ''')
    abstract Long removeTargetDataClass(@NonNull UUID id, UUID dataClassId)

    abstract List<DataClassComponent> readAllByDataFlow(DataFlow dataFlow)


    @Query(''' select * from datamodel.data_class dc where exists (select data_class_id from dataflow.data_class_component_source_data_class s
                where s.data_class_id = dc.id and s.data_class_component_id = :id) ''')
    @Nullable
    abstract List<DataClass> findAllSourceDataClasses(UUID id)


    @Query(''' select * from datamodel.data_class dc where exists (select data_class_id from dataflow.data_class_component_target_data_class t
                where t.data_class_id = dc.id and t.data_class_component_id = :id) ''')
    @Nullable
    abstract List<DataClass> findAllTargetDataClasses(@NonNull UUID id)


    @Query(''' delete from dataflow.data_class_component_source_data_class s where s.data_class_component_id = :id ''')
    @Nullable
    abstract Long removeSourceDataClasses(@NonNull UUID id)

    @Query(''' delete from dataflow.data_class_component_target_data_class t where t.data_class_component_id = :id ''')
    @Nullable
    abstract Long removeTargetDataClasses(@NonNull UUID id)
}

