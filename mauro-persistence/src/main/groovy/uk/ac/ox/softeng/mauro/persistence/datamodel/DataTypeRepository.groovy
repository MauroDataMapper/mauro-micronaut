package uk.ac.ox.softeng.mauro.persistence.datamodel

import uk.ac.ox.softeng.mauro.domain.datamodel.DataClass
import uk.ac.ox.softeng.mauro.domain.datamodel.DataModel
import uk.ac.ox.softeng.mauro.domain.datamodel.DataType
import uk.ac.ox.softeng.mauro.domain.model.AdministeredItem
import uk.ac.ox.softeng.mauro.persistence.datamodel.dto.DataTypeDTORepository
import uk.ac.ox.softeng.mauro.persistence.model.ModelItemRepository

import io.micronaut.core.annotation.Nullable
import io.micronaut.data.jdbc.annotation.JdbcRepository
import io.micronaut.data.model.query.builder.sql.Dialect
import jakarta.inject.Inject

@JdbcRepository(dialect = Dialect.POSTGRES)
abstract class DataTypeRepository implements ModelItemRepository<DataType> {

    @Inject
    DataTypeDTORepository dataTypeDTORepository

    @Override
    @Nullable
    DataType findById(UUID id) {
        dataTypeDTORepository.findById(id) as DataType
    }

    @Nullable
    List<DataType> findAllByDataModel(DataModel dataModel) {
        dataTypeDTORepository.findAllByDataModel(dataModel) as List<DataType>
    }

    @Nullable
    List<DataType> findAllByReferenceClassId(UUID referenceClassId) {
        dataTypeDTORepository.findAllByReferenceClassId(referenceClassId) as List<DataType>
    }

    @Override
    @Nullable
    List<DataType> findAllByParent(AdministeredItem parent) {
        findAllByDataModel((DataModel) parent)
    }

    @Nullable
    abstract List<DataType> readAllByDataModel(DataModel dataModel)

    @Override
    @Nullable
    List<DataType> readAllByParent(AdministeredItem parent) {
        readAllByDataModel((DataModel) parent)
    }

    abstract Long deleteByDataModelId(UUID dataModelId)

    //    @Override
    Long deleteByOwnerId(UUID ownerId) {
        deleteByDataModelId(ownerId)
    }

    @Nullable
    List<DataType> findAllByReferenceClass(UUID referenceClassId) {
        dataTypeDTORepository.findAllByReferenceClassId(referenceClassId)
    }

    @Override
    Class getDomainClass() {
        DataType
    }

    @Override
    Boolean handles(String domainType) {
        return domainType != null && domainType.toLowerCase() in
               ['datatype', 'datatypes', 'primitivetype', 'primitivetypes', 'enumerationtype', 'enumerationtypes', 'referencetype', 'referencetypes', 'modeltype',
                'modeltypes']
    }

}
