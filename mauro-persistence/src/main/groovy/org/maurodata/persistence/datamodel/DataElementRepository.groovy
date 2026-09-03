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
import org.maurodata.web.ListResponse
import org.maurodata.web.PaginationParams

@Slf4j
@CompileStatic
@JdbcRepository(dialect = Dialect.POSTGRES)
abstract class DataElementRepository implements ModelItemRepository<DataElement> {

    @Inject
    DataElementDTORepository dataElementDTORepository

    @Inject
    DataClassRepository dataClassRepository

    @Override
    @Nullable
    DataElement findById(UUID id) {
        log.debug 'DataElementRepository::findById'
        dataElementDTORepository.findById(id) as DataElement
    }

    @Nullable
    List<DataElement> findAllByParentAndPathIdentifier(UUID item,String pathIdentifier) {
        dataElementDTORepository.findAllByParentAndPathIdentifier(item,pathIdentifier) as List<DataElement>
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
        dataElementDTORepository.findAllByDataClassIdIn(dataClasses.collect{it.id}) as List<DataElement>
    }

    @Nullable
    List<DataElement> readAllByDataClassIn(Collection<DataClass> dataClasses) {
        dataElementDTORepository.readAllByDataClassIdIn(dataClasses.collect{it.id}) as List<DataElement>
    }

    @Nullable
    @Override
    List<DataElement> findAllByLabel(String label){
        dataElementDTORepository.findAllByLabel(label)
    }

    @Nullable
    List<DataElement> readAllByDataTypeIn(List<DataType> dataTypes) {
        dataElementDTORepository.readAllByDataTypeIdIn(dataTypes.id) as List<DataElement>
    }

    @Nullable
    abstract List<DataElement> readAllByDataClassIdIn(Collection<UUID> dataClassIds)

    @Nullable
    abstract List<UUID> readAllIdByDataClassIdIn(Collection<UUID> dataClassIds)

    @Nullable
    List<DataElement> findAllByDataClassDataModelIdIn(Collection<UUID> dataModelIds) {
        dataElementDTORepository.findAllByDataClassDataModelIdIn(dataModelIds) as List<DataElement>
    }

    @Nullable
    abstract List<DataElement> readAllByDataClassDataModelIdIn(Collection<UUID> dataModelIds)

    DataElement readByDataClassAndLabel(DataClass dataClass, String label) {
        dataElementDTORepository.readByDataClassAndLabel(dataClass, label) as DataElement
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

    @Override
    @Nullable
    ListResponse<DataElement> readListResponseByParent(AdministeredItem parent, @Nullable PaginationParams params) {
        readListResponseByDataClassId(((DataClass) parent).id, params)
    }

    @Nullable
    ListResponse<DataElement> readListResponseByDataClassId(UUID dataClassId, @Nullable PaginationParams params) {
        if (params == null) {
            return ListResponse.from(readAllByDataClassId(dataClassId), params)
        }
        if (params.code || params.definition || params.domainType) {
            return ListResponse.from([], 0)
        }

        String label = filterValue(params.label)
        String description = filterValue(params.description)
        Long total = countAllByDataClassIdAndFilters(dataClassId, label, description)
        List<DataElement> dataElements = readFilteredDataElements(dataClassId, label, description, params)

        ListResponse.from(dataElements, total)
    }

    @Nullable
    ListResponse<DataElement> readListResponseByDataModelId(UUID dataModelId, @Nullable PaginationParams params) {
        if (params == null) {
            return ListResponse.from(readAllByDataModelId(dataModelId), params)
        }
        if (params.code || params.definition || params.domainType) {
            return ListResponse.from([], 0)
        }

        String label = filterValue(params.label)
        String description = filterValue(params.description)
        String dataClass = filterValue(params.dataClass)
        Long total = countAllByDataModelIdAndFilters(dataModelId, label, description, dataClass)
        List<DataElement> dataElements = readFilteredDataElementsByDataModelId(dataModelId, label, description, dataClass, params)

        ListResponse.from(dataElements, total)
    }

    private List<DataElement> readFilteredDataElements(UUID dataClassId, String label, String description, PaginationParams params) {
        if ((params.max != null && params.max <= 0) || params.all?.equalsIgnoreCase(Boolean.TRUE.toString())) {
            return params.order?.equalsIgnoreCase('desc')
                ? readAllByDataClassIdAndFiltersDesc(dataClassId, label, description)
                : readAllByDataClassIdAndFiltersAsc(dataClassId, label, description)
        }

        Integer max = params.max ?: 50
        Integer offset = Math.max(0, params.offset ?: 0)
        params.order?.equalsIgnoreCase('desc')
            ? readAllByDataClassIdAndFiltersDesc(dataClassId, label, description, max, offset)
            : readAllByDataClassIdAndFiltersAsc(dataClassId, label, description, max, offset)
    }

    private List<DataElement> readFilteredDataElementsByDataModelId(UUID dataModelId, String label, String description, String dataClass, PaginationParams params) {
        if ((params.max != null && params.max <= 0) || params.all?.equalsIgnoreCase(Boolean.TRUE.toString())) {
            return params.order?.equalsIgnoreCase('desc')
                ? readAllByDataModelIdAndFiltersDesc(dataModelId, label, description, dataClass)
                : readAllByDataModelIdAndFiltersAsc(dataModelId, label, description, dataClass)
        }

        Integer max = params.max ?: 50
        Integer offset = Math.max(0, params.offset ?: 0)
        params.order?.equalsIgnoreCase('desc')
            ? readAllByDataModelIdAndFiltersDesc(dataModelId, label, description, dataClass, max, offset)
            : readAllByDataModelIdAndFiltersAsc(dataModelId, label, description, dataClass, max, offset)
    }

    private static String filterValue(String value) {
        value ? value : null
    }

    @Query('''SELECT count(*)
              FROM datamodel.data_element
              WHERE data_class_id = :dataClassId
                AND (:label IS NULL OR lower(label) LIKE concat('%', lower(:label), '%'))
                AND (:description IS NULL OR lower(description) LIKE concat('%', lower(:description), '%'))''')
    abstract Long countAllByDataClassIdAndFilters(UUID dataClassId, @Nullable String label, @Nullable String description)

    @Query('''SELECT count(*)
              FROM datamodel.data_element de
              JOIN datamodel.data_class dc ON de.data_class_id = dc.id
              WHERE dc.data_model_id = :dataModelId
                AND (:label IS NULL OR lower(de.label) LIKE concat('%', lower(:label), '%'))
                AND (:description IS NULL OR lower(de.description) LIKE concat('%', lower(:description), '%'))
                AND (:dataClass IS NULL OR lower(dc.label) LIKE concat('%', lower(:dataClass), '%'))''')
    abstract Long countAllByDataModelIdAndFilters(UUID dataModelId, @Nullable String label, @Nullable String description, @Nullable String dataClass)

    @Query('''SELECT *
              FROM datamodel.data_element
              WHERE data_class_id = :dataClassId
                AND (:label IS NULL OR lower(label) LIKE concat('%', lower(:label), '%'))
                AND (:description IS NULL OR lower(description) LIKE concat('%', lower(:description), '%'))
              ORDER BY idx ASC NULLS FIRST, label ASC NULLS FIRST''')
    abstract List<DataElement> readAllByDataClassIdAndFiltersAsc(UUID dataClassId, @Nullable String label, @Nullable String description)

    @Query('''SELECT *
              FROM datamodel.data_element
              WHERE data_class_id = :dataClassId
                AND (:label IS NULL OR lower(label) LIKE concat('%', lower(:label), '%'))
                AND (:description IS NULL OR lower(description) LIKE concat('%', lower(:description), '%'))
              ORDER BY idx DESC NULLS LAST, label DESC NULLS LAST''')
    abstract List<DataElement> readAllByDataClassIdAndFiltersDesc(UUID dataClassId, @Nullable String label, @Nullable String description)

    @Query('''SELECT *
              FROM datamodel.data_element
              WHERE data_class_id = :dataClassId
                AND (:label IS NULL OR lower(label) LIKE concat('%', lower(:label), '%'))
                AND (:description IS NULL OR lower(description) LIKE concat('%', lower(:description), '%'))
              ORDER BY idx ASC NULLS FIRST, label ASC NULLS FIRST
              LIMIT :max OFFSET :offset''')
    abstract List<DataElement> readAllByDataClassIdAndFiltersAsc(UUID dataClassId, @Nullable String label, @Nullable String description, Integer max, Integer offset)

    @Query('''SELECT *
              FROM datamodel.data_element
              WHERE data_class_id = :dataClassId
                AND (:label IS NULL OR lower(label) LIKE concat('%', lower(:label), '%'))
                AND (:description IS NULL OR lower(description) LIKE concat('%', lower(:description), '%'))
              ORDER BY idx DESC NULLS LAST, label DESC NULLS LAST
              LIMIT :max OFFSET :offset''')
    abstract List<DataElement> readAllByDataClassIdAndFiltersDesc(UUID dataClassId, @Nullable String label, @Nullable String description, Integer max, Integer offset)

    @Query('''SELECT de.*
              FROM datamodel.data_element de
              JOIN datamodel.data_class dc ON de.data_class_id = dc.id
              WHERE dc.data_model_id = :dataModelId
                AND (:label IS NULL OR lower(de.label) LIKE concat('%', lower(:label), '%'))
                AND (:description IS NULL OR lower(de.description) LIKE concat('%', lower(:description), '%'))
                AND (:dataClass IS NULL OR lower(dc.label) LIKE concat('%', lower(:dataClass), '%'))
              ORDER BY de.idx ASC NULLS FIRST, de.label ASC NULLS FIRST''')
    abstract List<DataElement> readAllByDataModelIdAndFiltersAsc(UUID dataModelId, @Nullable String label, @Nullable String description, @Nullable String dataClass)

    @Query('''SELECT de.*
              FROM datamodel.data_element de
              JOIN datamodel.data_class dc ON de.data_class_id = dc.id
              WHERE dc.data_model_id = :dataModelId
                AND (:label IS NULL OR lower(de.label) LIKE concat('%', lower(:label), '%'))
                AND (:description IS NULL OR lower(de.description) LIKE concat('%', lower(:description), '%'))
                AND (:dataClass IS NULL OR lower(dc.label) LIKE concat('%', lower(:dataClass), '%'))
              ORDER BY de.idx DESC NULLS LAST, de.label DESC NULLS LAST''')
    abstract List<DataElement> readAllByDataModelIdAndFiltersDesc(UUID dataModelId, @Nullable String label, @Nullable String description, @Nullable String dataClass)

    @Query('''SELECT de.*
              FROM datamodel.data_element de
              JOIN datamodel.data_class dc ON de.data_class_id = dc.id
              WHERE dc.data_model_id = :dataModelId
                AND (:label IS NULL OR lower(de.label) LIKE concat('%', lower(:label), '%'))
                AND (:description IS NULL OR lower(de.description) LIKE concat('%', lower(:description), '%'))
                AND (:dataClass IS NULL OR lower(dc.label) LIKE concat('%', lower(:dataClass), '%'))
              ORDER BY de.idx ASC NULLS FIRST, de.label ASC NULLS FIRST
              LIMIT :max OFFSET :offset''')
    abstract List<DataElement> readAllByDataModelIdAndFiltersAsc(UUID dataModelId, @Nullable String label, @Nullable String description, @Nullable String dataClass,
                                                                 Integer max, Integer offset)

    @Query('''SELECT de.*
              FROM datamodel.data_element de
              JOIN datamodel.data_class dc ON de.data_class_id = dc.id
              WHERE dc.data_model_id = :dataModelId
                AND (:label IS NULL OR lower(de.label) LIKE concat('%', lower(:label), '%'))
                AND (:description IS NULL OR lower(de.description) LIKE concat('%', lower(:description), '%'))
                AND (:dataClass IS NULL OR lower(dc.label) LIKE concat('%', lower(:dataClass), '%'))
              ORDER BY de.idx DESC NULLS LAST, de.label DESC NULLS LAST
              LIMIT :max OFFSET :offset''')
    abstract List<DataElement> readAllByDataModelIdAndFiltersDesc(UUID dataModelId, @Nullable String label, @Nullable String description, @Nullable String dataClass,
                                                                  Integer max, Integer offset)

    @Nullable
    @Query('''select de.* from datamodel.data_element de join datamodel.data_class dc on (de.data_class_id=dc.id)
              where dc.data_model_id = :dataModelId''')
    abstract List<DataElement> readAllByDataModelId(UUID dataModelId)

    abstract Long deleteByDataClassId(UUID dataClassId)

    abstract List<DataElement> readAllByDataClassDataModelIdInAndLabelContains(Collection<UUID> dataModelIds, String label)

    //    @Override
    Long deleteByOwnerId(UUID ownerId) {
        deleteByDataClassId(ownerId)
    }

    @Override
    Class getDomainClass() {
        DataElement
    }

    abstract List<DataElement> readAllByDataClassId(UUID dataClassId)

    Boolean handlesPathPrefix(final String pathPrefix) {
        'de'.equalsIgnoreCase(pathPrefix)
    }

}
