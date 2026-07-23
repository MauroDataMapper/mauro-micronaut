package org.maurodata.persistence.datamodel

import org.maurodata.domain.datamodel.DataClass
import org.maurodata.domain.datamodel.DataModel
import org.maurodata.domain.model.AdministeredItem
import org.maurodata.persistence.datamodel.dto.DataClassDTORepository

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import io.micronaut.core.annotation.NonNull
import io.micronaut.core.annotation.Nullable
import io.micronaut.data.annotation.Query
import io.micronaut.data.jdbc.annotation.JdbcRepository
import io.micronaut.data.model.query.builder.sql.Dialect
import jakarta.inject.Inject
import org.maurodata.persistence.datamodel.dto.DataClassExtensionDTO
import org.maurodata.persistence.model.ModelItemRepository
import org.maurodata.web.ListResponse
import org.maurodata.web.PaginationParams

@Slf4j
@CompileStatic
@JdbcRepository(dialect = Dialect.POSTGRES)
abstract class DataClassRepository implements ModelItemRepository<DataClass> {

    @Inject
    DataClassDTORepository dataClassDTORepository

    @Override
    @Nullable
    DataClass findById(UUID id) {
        // log.debug 'DataClassRepository::findById'
        dataClassDTORepository.findById(id) as DataClass
    }

    @Nullable
    List<DataClass> findAllByParentAndPathIdentifier(UUID item,String pathIdentifier) {
        dataClassDTORepository.findAllByParentAndPathIdentifier(item,pathIdentifier) as List<DataClass>
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



    @Nullable
    @Override
    List<DataClass> findAllByLabel(String label) {
        dataClassDTORepository.findAllByLabel(label)
    }

    @Nullable
    abstract List<DataClass> readAllByDataModel(DataModel dataModel)

    @Nullable
    abstract List<DataClass> readAllByDataModelAndParentDataClassIsNull(DataModel dataModel)

    @Nullable
    abstract DataClass readByDataModelAndLabelAndParentDataClassIsNull(DataModel dataModel, String label)

    @Nullable
    abstract List<DataClass> readAllByDataModelIdInAndParentDataClassIsNull(Collection<UUID> dataModelIds)

    @Nullable
    abstract List<UUID> readAllIdByDataModelIdInAndParentDataClassIsNull(Collection<UUID> dataModelIds)

    @Nullable
    abstract List<DataClass> readAllByParentDataClass_Id(UUID dataClassId)

    @Nullable
    abstract List<DataClass> readAllByParentDataClass(DataClass parentDataClass)

    @Nullable
    abstract List<DataClass> readAllByParentDataClassIdIn(Collection<UUID> dataClassIds)

    @Nullable
    abstract List<UUID> readAllIdByParentDataClassIdIn(Collection<UUID> dataClassIds)

    @Nullable
    abstract DataClass readByParentDataClassAndLabel(DataClass parentDataClass, String label)

    @Query('''delete from datamodel.join_dataclass_to_extended_data_class jdcedc where jdcedc.dataclass_id = :dataClassId and jdcedc.extended_dataclass_id = :extendedDataClassId''')
    abstract long deleteExtensionRelationship(@NonNull UUID dataClassId, @NonNull UUID extendedDataClassId)

    @Query('''delete from datamodel.join_dataclass_to_extended_data_class jdcedc where jdcedc.dataclass_id in (:dataClassIds)''')
    abstract long deleteExtensionRelationships(List<UUID> dataClassIds)

    @Query('''insert into datamodel.join_dataclass_to_extended_data_class (dataclass_id, extended_dataclass_id) values (:dataClassId, :extendedDataClassId)''')
    abstract DataClass addDataClassExtensionRelationship(@NonNull UUID dataClassId, @NonNull UUID extendedDataClassId)

    @Query('''select data_class.* from datamodel.join_dataclass_to_extended_data_class jdcedc (dataclass_id, extended_dataclass_id) inner join datamodel.data_class on jdcedc.extended_dataclass_id = id where dataclass_id = :dataClassId''')
    abstract List<DataClass> getDataClassExtensionRelationships(@NonNull UUID dataClassId)

    @Query('''select dataclass_id AS data_class_id,
           extended_dataclass_id AS extended_data_class_id from datamodel.join_dataclass_to_extended_data_class where dataclass_id in (:dataClassIds)''')
    abstract List<DataClassExtensionDTO> getDataClassExtensionRelationships(@NonNull List<UUID> dataClassIds)

    @Override
    @Nullable
    List<DataClass> readAllByParent(AdministeredItem parent) {
        readAllByDataModel((DataModel) parent)
    }

    @Nullable
    ListResponse<DataClass> readListResponseByDataModelAndParentDataClassIsNull(DataModel dataModel, @Nullable PaginationParams params) {
        if (params == null) {
            return ListResponse.from(readAllByDataModelAndParentDataClassIsNull(dataModel), params)
        }
        if (params.code || params.definition || params.domainType) {
            return ListResponse.from([], 0)
        }

        String label = filterValue(params.label)
        String description = filterValue(params.description)
        Long total = countAllByDataModelIdAndParentDataClassIsNullAndFilters(dataModel.id, label, description)
        List<DataClass> dataClasses

        if ((params.max != null && params.max <= 0) || params.all?.equalsIgnoreCase(Boolean.TRUE.toString())) {
            dataClasses = params.order?.equalsIgnoreCase('desc')
                ? readAllByDataModelIdAndParentDataClassIsNullAndFiltersDesc(dataModel.id, label, description)
                : readAllByDataModelIdAndParentDataClassIsNullAndFiltersAsc(dataModel.id, label, description)
        } else {
            Integer max = params.max ?: 50
            Integer offset = Math.max(0, params.offset ?: 0)
            dataClasses = params.order?.equalsIgnoreCase('desc')
                ? readAllByDataModelIdAndParentDataClassIsNullAndFiltersDesc(dataModel.id, label, description, max, offset)
                : readAllByDataModelIdAndParentDataClassIsNullAndFiltersAsc(dataModel.id, label, description, max, offset)
        }

        ListResponse.from(dataClasses, total)
    }

    private static String filterValue(String value) {
        value ? value : null
    }

    @Query('''SELECT count(*)
              FROM datamodel.data_class
              WHERE data_model_id = :dataModelId
                AND parent_data_class_id IS NULL
                AND (:label IS NULL OR lower(label) LIKE concat('%', lower(:label), '%'))
                AND (:description IS NULL OR lower(description) LIKE concat('%', lower(:description), '%'))''')
    abstract Long countAllByDataModelIdAndParentDataClassIsNullAndFilters(UUID dataModelId, @Nullable String label, @Nullable String description)

    @Query('''SELECT *
              FROM datamodel.data_class
              WHERE data_model_id = :dataModelId
                AND parent_data_class_id IS NULL
                AND (:label IS NULL OR lower(label) LIKE concat('%', lower(:label), '%'))
                AND (:description IS NULL OR lower(description) LIKE concat('%', lower(:description), '%'))
              ORDER BY idx ASC NULLS FIRST, label ASC NULLS FIRST''')
    abstract List<DataClass> readAllByDataModelIdAndParentDataClassIsNullAndFiltersAsc(UUID dataModelId, @Nullable String label, @Nullable String description)

    @Query('''SELECT *
              FROM datamodel.data_class
              WHERE data_model_id = :dataModelId
                AND parent_data_class_id IS NULL
                AND (:label IS NULL OR lower(label) LIKE concat('%', lower(:label), '%'))
                AND (:description IS NULL OR lower(description) LIKE concat('%', lower(:description), '%'))
              ORDER BY idx DESC NULLS LAST, label DESC NULLS LAST''')
    abstract List<DataClass> readAllByDataModelIdAndParentDataClassIsNullAndFiltersDesc(UUID dataModelId, @Nullable String label, @Nullable String description)

    @Query('''SELECT *
              FROM datamodel.data_class
              WHERE data_model_id = :dataModelId
                AND parent_data_class_id IS NULL
                AND (:label IS NULL OR lower(label) LIKE concat('%', lower(:label), '%'))
                AND (:description IS NULL OR lower(description) LIKE concat('%', lower(:description), '%'))
              ORDER BY idx ASC NULLS FIRST, label ASC NULLS FIRST
              LIMIT :max OFFSET :offset''')
    abstract List<DataClass> readAllByDataModelIdAndParentDataClassIsNullAndFiltersAsc(UUID dataModelId, @Nullable String label, @Nullable String description,
                                                                                       Integer max, Integer offset)

    @Query('''SELECT *
              FROM datamodel.data_class
              WHERE data_model_id = :dataModelId
                AND parent_data_class_id IS NULL
                AND (:label IS NULL OR lower(label) LIKE concat('%', lower(:label), '%'))
                AND (:description IS NULL OR lower(description) LIKE concat('%', lower(:description), '%'))
              ORDER BY idx DESC NULLS LAST, label DESC NULLS LAST
              LIMIT :max OFFSET :offset''')
    abstract List<DataClass> readAllByDataModelIdAndParentDataClassIsNullAndFiltersDesc(UUID dataModelId, @Nullable String label, @Nullable String description,
                                                                                        Integer max, Integer offset)

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
