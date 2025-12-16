package org.maurodata.persistence.datamodel

import io.micronaut.core.annotation.Nullable
import io.micronaut.data.jdbc.annotation.JdbcRepository
import io.micronaut.data.model.query.builder.sql.Dialect
import jakarta.inject.Inject
import org.maurodata.domain.datamodel.DataModel
import org.maurodata.domain.datamodel.DataType
import org.maurodata.domain.model.AdministeredItem
import org.maurodata.persistence.datamodel.dto.DataTypeDTORepository
import org.maurodata.persistence.model.ModelItemRepository

@JdbcRepository(dialect = Dialect.POSTGRES)
abstract class DataTypeRepository implements ModelItemRepository<DataType> {

    @Inject
    DataTypeDTORepository dataTypeDTORepository

    @Nullable
    List<DataType> findAllByParentAndPathIdentifier(UUID item,String pathIdentifier) {
        dataTypeDTORepository.findAllByParentAndPathIdentifier(item,pathIdentifier)
    }

    @Override
    @Nullable
    DataType findById(UUID id) {
        dataTypeDTORepository.findById(id) as DataType
    }

    @Override
    @Nullable
    List<DataType> findAllByLabel(String pathIdentifier){
        dataTypeDTORepository.findAllByLabel(pathIdentifier)
    }
    @Nullable
    List<DataType> findAllByDataModel(DataModel dataModel) {
        dataTypeDTORepository.findAllByDataModel(dataModel) as List<DataType>
    }


    @Nullable
    List<DataType> findByReferenceClassIn(List<UUID> referenceClassIds){
        dataTypeDTORepository.findByReferenceClassIdIn(referenceClassIds) as List<DataType>
    }

    @Override
    @Nullable
    List<DataType> findAllByParent(AdministeredItem parent) {
        findAllByDataModel((DataModel) parent)
    }

    @Nullable
    abstract List<DataType> readAllByDataModel(DataModel dataModel)

    @Nullable
    abstract List<DataType> readAllByDataModelIdIn(Collection<UUID> dataModelIds)

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
               ['datatype', 'datatypes', 'primitivetype', 'primitivetypes', 'enumerationtype', 'enumerationtypes', 'referencetype', 'referencetypes', 'modeldatatype',
                'modeldatatypes']
    }

    Boolean handlesPathPrefix(final String pathPrefix) {
        'dt'.equalsIgnoreCase(pathPrefix)
    }
}
