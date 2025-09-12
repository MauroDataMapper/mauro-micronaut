package org.maurodata.persistence.datamodel

import org.maurodata.domain.datamodel.DataClass
import org.maurodata.domain.datamodel.DataModel
import org.maurodata.domain.model.AdministeredItem
import org.maurodata.persistence.datamodel.dto.DataClassDTORepository
import org.maurodata.persistence.model.ModelItemRepository

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import io.micronaut.core.annotation.NonNull
import io.micronaut.core.annotation.Nullable
import io.micronaut.data.annotation.Query
import io.micronaut.data.jdbc.annotation.JdbcRepository
import io.micronaut.data.model.query.builder.sql.Dialect
import jakarta.inject.Inject

@Slf4j
@CompileStatic
@JdbcRepository(dialect = Dialect.POSTGRES)
abstract class DataClassRepository implements ModelItemRepository<DataClass> {

    @Inject
    DataClassDTORepository dataClassDTORepository

    @Override
    @Nullable
    DataClass findById(UUID id) {
        log.debug 'DataClassRepository::findById'
        dataClassDTORepository.findById(id) as DataClass
    }

    @Nullable
    List<DataClass> findAllByParentAndPathIdentifier(UUID item,String pathIdentifier) {
        dataClassDTORepository.findAllByParentAndPathIdentifier(item,pathIdentifier)
    }

    @Nullable
    List<DataClass> findAllByDataModel(DataModel dataModel) {
        dataClassDTORepository.findAllByDataModel(dataModel) as List<DataClass>
    }

    @Override
    @Nullable
    List<DataClass> findAllByParent(AdministeredItem parent) {
        findAllByDataModel((DataModel) parent)
    }

    DataClass findByPathIdentifier(String pathIdentifier) {
        dataClassDTORepository.findByLabel(pathIdentifier)
    }
    @Nullable
    abstract List<DataClass> readAllByDataModel(DataModel dataModel)

    @Nullable
    abstract List<DataClass> readAllByDataModelAndParentDataClassIsNull(DataModel dataModel)

    @Nullable
    abstract List<DataClass> readAllByDataModel_Id(UUID dataModelId)


    @Nullable
    abstract List<DataClass> readAllByParentDataClass_Id(UUID dataClassId)

    @Nullable
    abstract List<DataClass> readAllByParentDataClass(DataClass parentDataClass)

    @Query('''delete from datamodel.join_dataclass_to_extended_data_class jdcedc where jdcedc.dataclass_id = :dataClassId and jdcedc.extended_dataclass_id = :extendedDataClassId''')
    abstract long deleteExtensionRelationship(@NonNull UUID dataClassId, @NonNull UUID extendedDataClassId)

    @Query('''delete from datamodel.join_dataclass_to_extended_data_class jdcedc where jdcedc.dataclass_id in (:dataClassIds)''')
    abstract long deleteExtensionRelationships(List<UUID> dataClassIds)

    @Query('''insert into datamodel.join_dataclass_to_extended_data_class (dataclass_id, extended_dataclass_id) values (:dataClassId, :extendedDataClassId)''')
    abstract DataClass addDataClassExtensionRelationship(@NonNull UUID dataClassId, @NonNull UUID extendedDataClassId)

    @Query('''select from datamodel.join_dataclass_to_extended_data_class jdcedc (dataclass_id, extended_dataclass_id) inner join datamodel.data_class on jdcedc.dataclass_id = id where dataclass_id = :dataClassId''')
    abstract List<DataClass> getDataClassExtensionRelationships(@NonNull UUID dataClassId)

    @Override
    @Nullable
    List<DataClass> readAllByParent(AdministeredItem parent) {
        readAllByDataModel((DataModel) parent)
    }

    abstract Long deleteByDataModelId(UUID dataModelId)

    //    @Override
    Long deleteByOwnerId(UUID ownerId) {
        deleteByDataModelId(ownerId)
    }


    @Override
    Class getDomainClass() {
        DataClass
    }

    @Override
    Boolean handles(Class clazz) {
        domainClass.isAssignableFrom(clazz)
    }

    @Override
    Boolean handles(String domainType) {
        domainClass.simpleName.equalsIgnoreCase(domainType) || (domainClass.simpleName + 'es').equalsIgnoreCase(domainType)
    }

    Boolean handlesPathPrefix(final String pathPrefix) {
        'dc'.equalsIgnoreCase(pathPrefix)
    }
}

