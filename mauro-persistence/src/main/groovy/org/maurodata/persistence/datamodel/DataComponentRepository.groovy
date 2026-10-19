package org.maurodata.persistence.datamodel

import groovy.transform.CompileStatic
import io.micronaut.core.annotation.Nullable
import io.micronaut.data.annotation.Query
import io.micronaut.data.jdbc.annotation.JdbcRepository
import io.micronaut.data.model.query.builder.sql.Dialect
import jakarta.inject.Inject
import org.maurodata.domain.datamodel.DataComponent
import org.maurodata.domain.datamodel.DataModel
import org.maurodata.domain.model.AdministeredItem
import org.maurodata.persistence.datamodel.dto.DataComponentDTORepository
import org.maurodata.persistence.model.ModelItemRepository

@CompileStatic
@JdbcRepository(dialect = Dialect.POSTGRES)
abstract class DataComponentRepository implements ModelItemRepository<DataComponent> {

    @Inject DataComponentDTORepository dataComponentDTORepository

    @Override
    @Nullable
    DataComponent findById(UUID id) {
        dataComponentDTORepository.findById(id) as DataComponent
    }

    @Override
    @Nullable
    DataComponent readById(UUID id) {
        dataComponentDTORepository.findById(id) as DataComponent
    }

    @Override
    @Nullable
    List<DataComponent> findAllByParent(AdministeredItem parent) {
        dataComponentDTORepository.findAllByDataModel((DataModel) parent) as List<DataComponent>
    }

    @Override
    @Nullable
    List<DataComponent> readAllByParent(AdministeredItem parent) {
        readAllByDataModel((DataModel) parent)
    }

    @Override
    @Nullable
    List<DataComponent> findAllByParentAndPathIdentifier(UUID parentId, String pathIdentifier) {
        dataComponentDTORepository.findAllByParentAndPathIdentifier(parentId, pathIdentifier)
    }

    @Override
    @Nullable
    List<DataComponent> findAllByLabel(String label) {
        dataComponentDTORepository.findAllByLabel(label)
    }

    @Nullable
    abstract List<DataComponent> readAllByDataModel(DataModel dataModel)

    @Nullable
    abstract List<UUID> readAllIdByDataModelIdIn(Collection<UUID> dataModelIds)

    @Nullable
    abstract List<DataComponent> readAllByDataModelIdIn(Collection<UUID> dataModelIds)

    @Override
    Class<DataComponent> getDomainClass() { DataComponent }

    @Override
    Boolean handlesPathPrefix(String pathPrefix) { 'dcp'.equalsIgnoreCase(pathPrefix) }
}
