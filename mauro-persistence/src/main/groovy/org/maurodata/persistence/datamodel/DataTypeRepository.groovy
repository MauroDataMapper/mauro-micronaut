package org.maurodata.persistence.datamodel

import groovy.transform.CompileStatic
import io.micronaut.core.annotation.Nullable
import io.micronaut.data.annotation.Query
import io.micronaut.data.jdbc.annotation.JdbcRepository
import io.micronaut.data.model.query.builder.sql.Dialect
import jakarta.inject.Inject
import org.maurodata.domain.datamodel.DataModel
import org.maurodata.domain.datamodel.DataType
import org.maurodata.domain.model.AdministeredItem
import org.maurodata.persistence.datamodel.dto.DataTypeDTORepository
import org.maurodata.persistence.model.ModelItemRepository
import org.maurodata.web.ListResponse
import org.maurodata.web.PaginationParams

@CompileStatic
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

    @Nullable
    abstract List<UUID> readAllIdByDataModelIdIn(Collection<UUID> dataModelIds)

    @Override
    @Nullable
    List<DataType> readAllByParent(AdministeredItem parent) {
        readAllByDataModel((DataModel) parent)
    }

    @Override
    @Nullable
    ListResponse<DataType> readListResponseByParent(AdministeredItem parent, @Nullable PaginationParams params) {
        if (params == null) {
            return ListResponse.from(readAllByParent(parent), params)
        }

        // ListResponse filters code/definition to Term instances only, so these filters exclude DataType rows.
        if (params.code || params.definition) {
            return ListResponse.from([], 0)
        }

        UUID dataModelId = parent.id
        String label = filterValue(params.label)
        String description = filterValue(params.description)
        String domainType = filterValue(params.domainType)
        Long total = countAllByDataModelIdAndFilters(dataModelId, label, description, domainType)
        List<DataType> dataTypes

        if (params.max <= 0 || params.all?.equalsIgnoreCase(Boolean.TRUE.toString())) {
            dataTypes = params.order?.equalsIgnoreCase('desc')
                ? readAllByDataModelIdAndFiltersDesc(dataModelId, label, description, domainType)
                : readAllByDataModelIdAndFiltersAsc(dataModelId, label, description, domainType)
        } else {
            Integer max = params.max ?: 50
            Integer offset = Math.max(0, params.offset ?: 0)
            dataTypes = params.order?.equalsIgnoreCase('desc')
                ? readAllByDataModelIdAndFiltersDesc(dataModelId, label, description, domainType, max, offset)
                : readAllByDataModelIdAndFiltersAsc(dataModelId, label, description, domainType, max, offset)
        }

        ListResponse.from(dataTypes, total)
    }

    private static String filterValue(String value) {
        value ? value : null
    }

    @Query('''SELECT count(*)
              FROM datamodel.data_type
              WHERE data_model_id = :dataModelId
                AND (:label IS NULL OR lower(label) LIKE concat('%', lower(:label), '%'))
                AND (:description IS NULL OR lower(description) LIKE concat('%', lower(:description), '%'))
                AND (:domainType IS NULL OR lower(domain_type) LIKE concat('%', lower(:domainType), '%'))''')
    abstract Long countAllByDataModelIdAndFilters(UUID dataModelId, @Nullable String label, @Nullable String description, @Nullable String domainType)

    @Query('''SELECT *
              FROM datamodel.data_type
              WHERE data_model_id = :dataModelId
                AND (:label IS NULL OR lower(label) LIKE concat('%', lower(:label), '%'))
                AND (:description IS NULL OR lower(description) LIKE concat('%', lower(:description), '%'))
                AND (:domainType IS NULL OR lower(domain_type) LIKE concat('%', lower(:domainType), '%'))
              ORDER BY idx ASC NULLS FIRST, label ASC NULLS FIRST''')
    abstract List<DataType> readAllByDataModelIdAndFiltersAsc(UUID dataModelId, @Nullable String label, @Nullable String description, @Nullable String domainType)

    @Query('''SELECT *
              FROM datamodel.data_type
              WHERE data_model_id = :dataModelId
                AND (:label IS NULL OR lower(label) LIKE concat('%', lower(:label), '%'))
                AND (:description IS NULL OR lower(description) LIKE concat('%', lower(:description), '%'))
                AND (:domainType IS NULL OR lower(domain_type) LIKE concat('%', lower(:domainType), '%'))
              ORDER BY idx DESC NULLS LAST, label DESC NULLS LAST''')
    abstract List<DataType> readAllByDataModelIdAndFiltersDesc(UUID dataModelId, @Nullable String label, @Nullable String description, @Nullable String domainType)

    @Query('''SELECT *
              FROM datamodel.data_type
              WHERE data_model_id = :dataModelId
                AND (:label IS NULL OR lower(label) LIKE concat('%', lower(:label), '%'))
                AND (:description IS NULL OR lower(description) LIKE concat('%', lower(:description), '%'))
                AND (:domainType IS NULL OR lower(domain_type) LIKE concat('%', lower(:domainType), '%'))
              ORDER BY idx ASC NULLS FIRST, label ASC NULLS FIRST
              LIMIT :max OFFSET :offset''')
    abstract List<DataType> readAllByDataModelIdAndFiltersAsc(UUID dataModelId, @Nullable String label, @Nullable String description, @Nullable String domainType, Integer max, Integer offset)

    @Query('''SELECT *
              FROM datamodel.data_type
              WHERE data_model_id = :dataModelId
                AND (:label IS NULL OR lower(label) LIKE concat('%', lower(:label), '%'))
                AND (:description IS NULL OR lower(description) LIKE concat('%', lower(:description), '%'))
                AND (:domainType IS NULL OR lower(domain_type) LIKE concat('%', lower(:domainType), '%'))
              ORDER BY idx DESC NULLS LAST, label DESC NULLS LAST
              LIMIT :max OFFSET :offset''')
    abstract List<DataType> readAllByDataModelIdAndFiltersDesc(UUID dataModelId, @Nullable String label, @Nullable String description, @Nullable String domainType, Integer max, Integer offset)
    abstract Long deleteByDataModelId(UUID dataModelId)

    //    @Override
    Long deleteByOwnerId(UUID ownerId) {
        deleteByDataModelId(ownerId)
    }

    @Nullable
    List<DataType> findAllByReferenceClass(UUID referenceClassId) {
        dataTypeDTORepository.findAllByReferenceClassId(referenceClassId) as List<DataType>
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
