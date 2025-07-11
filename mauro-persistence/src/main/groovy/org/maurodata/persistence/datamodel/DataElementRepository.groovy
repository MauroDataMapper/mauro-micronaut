package org.maurodata.persistence.datamodel

import org.maurodata.domain.datamodel.DataType

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import io.micronaut.core.annotation.Nullable
import io.micronaut.data.annotation.Join
import io.micronaut.data.annotation.Query
import io.micronaut.data.jdbc.annotation.JdbcRepository
import io.micronaut.data.model.query.builder.sql.Dialect
import jakarta.inject.Inject
import org.maurodata.domain.datamodel.DataClass
import org.maurodata.domain.datamodel.DataElement
import org.maurodata.domain.model.AdministeredItem
import org.maurodata.persistence.datamodel.dto.DataElementDTORepository
import org.maurodata.persistence.model.ModelItemRepository

@Slf4j
@CompileStatic
@JdbcRepository(dialect = Dialect.POSTGRES)
abstract class DataElementRepository implements  ModelItemRepository<DataElement> {

    @Inject
    DataElementDTORepository dataElementDTORepository

    @Override
    @Nullable
    DataElement findById(UUID id) {
        log.debug 'DataElementRepository::findById'
        dataElementDTORepository.findById(id) as DataElement
    }

    @Nullable
    List<DataElement> findAllByDataClass(DataClass dataClass) {
        dataElementDTORepository.findAllByDataClass(dataClass) as List<DataElement>
    }

    @Override
    @Nullable
    List<DataElement> findAllByParent(AdministeredItem parent) {
        findAllByDataClass((DataClass) parent)
    }

    @Nullable
    List<DataElement> findAllByDataClassIn(Collection<DataClass> dataClasses) {
        dataElementDTORepository.findAllByDataClassIn(dataClasses) as List<DataElement>
    }

    @Nullable
    List<DataElement> readAllByDataTypeIn(List<DataType> dataTypes) {
        dataElementDTORepository.readAllByDataTypeIdIn(dataTypes.id) as List<DataElement>
    }

    @Nullable
    @Join(value = 'dataType', type = Join.Type.LEFT_FETCH)
    abstract List<DataElement> readAllByDataClass(DataClass dataClass)

    @Override
    @Nullable
    @Join(value = 'dataType', type = Join.Type.LEFT_FETCH)
    List<DataElement> readAllByParent(AdministeredItem parent) {
        readAllByDataClass((DataClass) parent)
    }

    @Nullable
    @Query('''select de.* from datamodel.data_element de join datamodel.data_class dc on (de.data_class_id=dc.id)
              where dc.data_model_id = :dataModelId''')
    abstract List<DataElement> readAllByDataModelId(UUID dataModelId)

    abstract Long deleteByDataClassId(UUID dataClassId)

    //    @Override
    Long deleteByOwnerId(UUID ownerId) {
        deleteByDataClassId(ownerId)
    }

    @Override
    Class getDomainClass() {
        DataElement
    }

    abstract List<DataElement> readAllByDataClassId(UUID dataClassId)

}
