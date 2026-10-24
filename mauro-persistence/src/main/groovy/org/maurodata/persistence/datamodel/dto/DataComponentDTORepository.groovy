package org.maurodata.persistence.datamodel.dto

import groovy.transform.CompileStatic
import io.micronaut.core.annotation.Nullable
import io.micronaut.data.annotation.Query
import io.micronaut.data.jdbc.annotation.JdbcRepository
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.repository.GenericRepository
import org.maurodata.domain.datamodel.DataComponent
import org.maurodata.domain.datamodel.DataModel

@CompileStatic
@JdbcRepository(dialect = Dialect.POSTGRES)
abstract class DataComponentDTORepository implements GenericRepository<DataComponentDTO, UUID> {

    @Nullable
    abstract DataComponentDTO findById(UUID id)

    @Nullable
    abstract List<DataComponentDTO> findAllByDataModel(DataModel dataModel)

    @Nullable
    @Query("select * from datamodel.data_component where data_model_id = :parentId and type || ': ' || label = :pathIdentifier")
    abstract List<DataComponent> findAllByParentAndPathIdentifier(UUID parentId, String pathIdentifier)

    @Nullable
    @Query("select * from datamodel.data_component where label = :label or type || ': ' || label = :label")
    abstract List<DataComponent> findAllByLabel(String label)
}
