package org.maurodata.persistence.datamodel

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import io.micronaut.core.annotation.Nullable
import io.micronaut.data.annotation.Query
import io.micronaut.data.jdbc.annotation.JdbcRepository
import io.micronaut.data.model.query.builder.sql.Dialect
import jakarta.inject.Inject
import org.maurodata.domain.datamodel.DataType
import org.maurodata.domain.datamodel.EnumerationValue
import org.maurodata.domain.model.AdministeredItem
import org.maurodata.persistence.datamodel.dto.EnumerationValueDTORepository
import org.maurodata.persistence.model.ModelItemRepository
import org.maurodata.web.ListResponse
import org.maurodata.web.PaginationParams

@Slf4j
@CompileStatic
@JdbcRepository(dialect = Dialect.POSTGRES)
abstract class EnumerationValueRepository implements ModelItemRepository<EnumerationValue> {

    @Inject
    EnumerationValueDTORepository enumerationValueDTORepository

    @Override
    @Nullable
    EnumerationValue findById(UUID id) {
        log.debug 'EnumerationValueRepository::findById'
        enumerationValueDTORepository.findById(id) as EnumerationValue
    }

    @Nullable
    List<EnumerationValue> findAllByParentAndPathIdentifier(UUID item,String pathIdentifier) {
        enumerationValueDTORepository.findAllByParentAndPathIdentifier(item,pathIdentifier) as List<EnumerationValue>
    }

    @Nullable
    List<EnumerationValue> findAllByEnumerationType(DataType dataType) {
        enumerationValueDTORepository.findAllByEnumerationType(dataType) as List<EnumerationValue>
    }

    @Override
    @Nullable
    List<EnumerationValue> findAllByParent(AdministeredItem parent) {
        findAllByEnumerationType((DataType) parent)
    }

    @Override
    @Nullable
    List<EnumerationValue> findAllByLabel(String label){
        enumerationValueDTORepository.findAllByLabel(label)
    }
    @Nullable
    abstract Set<EnumerationValue> readAllByEnumerationTypeIn(Collection<DataType> dataTypes)

    @Nullable
    abstract List<UUID> readAllIdByEnumerationTypeIdIn(Collection<UUID> dataTypeIds)

    @Nullable
    abstract List<EnumerationValue> readAllByEnumerationTypeIdIn(Collection<UUID> dataTypeIds)

    Set<EnumerationValue> findAllByEnumerationTypeIn(Collection<DataType> dataTypes) {
        enumerationValueDTORepository.findAllByEnumerationTypeIn(dataTypes) as Set<EnumerationValue>
    }

    @Nullable
    abstract List<EnumerationValue> readAllByEnumerationType(DataType dataType)

    @Override
    @Nullable
    List<EnumerationValue> readAllByParent(AdministeredItem parent) {
        readAllByEnumerationType((DataType) parent)
    }

    @Override
    @Nullable
    ListResponse<EnumerationValue> readListResponseByParent(AdministeredItem parent, @Nullable PaginationParams params) {
        readListResponseByEnumerationTypeId(parent.id, params)
    }

    @Nullable
    ListResponse<EnumerationValue> readListResponseByEnumerationTypeId(UUID enumerationTypeId, @Nullable PaginationParams params) {
        if (params == null) {
            return ListResponse.from(readAllByEnumerationTypeId(enumerationTypeId), params)
        }
        if (params.code || params.definition || params.domainType) {
            return ListResponse.from([], 0)
        }

        String label = filterValue(params.label)
        String description = filterValue(params.description)
        Long total = countAllByEnumerationTypeIdAndFilters(enumerationTypeId, label, description)
        List<EnumerationValue> enumerationValues

        if ((params.max != null && params.max <= 0) || params.all?.equalsIgnoreCase(Boolean.TRUE.toString())) {
            enumerationValues = params.order?.equalsIgnoreCase('desc')
                ? readAllByEnumerationTypeIdAndFiltersDesc(enumerationTypeId, label, description)
                : readAllByEnumerationTypeIdAndFiltersAsc(enumerationTypeId, label, description)
        } else {
            Integer max = params.max ?: 50
            Integer offset = Math.max(0, params.offset ?: 0)
            enumerationValues = params.order?.equalsIgnoreCase('desc')
                ? readAllByEnumerationTypeIdAndFiltersDesc(enumerationTypeId, label, description, max, offset)
                : readAllByEnumerationTypeIdAndFiltersAsc(enumerationTypeId, label, description, max, offset)
        }

        ListResponse.from(enumerationValues, total)
    }

    private static String filterValue(String value) {
        value ? value : null
    }

    @Query('''SELECT count(*)
              FROM datamodel.enumeration_value
              WHERE enumeration_type_id = :enumerationTypeId
                AND (:label IS NULL OR lower(label) LIKE concat('%', lower(:label), '%'))
                AND (:description IS NULL OR lower(description) LIKE concat('%', lower(:description), '%'))''')
    abstract Long countAllByEnumerationTypeIdAndFilters(UUID enumerationTypeId, @Nullable String label, @Nullable String description)

    @Query('''SELECT *
              FROM datamodel.enumeration_value
              WHERE enumeration_type_id = :enumerationTypeId
                AND (:label IS NULL OR lower(label) LIKE concat('%', lower(:label), '%'))
                AND (:description IS NULL OR lower(description) LIKE concat('%', lower(:description), '%'))
              ORDER BY idx ASC NULLS FIRST, label ASC NULLS FIRST''')
    abstract List<EnumerationValue> readAllByEnumerationTypeIdAndFiltersAsc(UUID enumerationTypeId, @Nullable String label, @Nullable String description)

    @Query('''SELECT *
              FROM datamodel.enumeration_value
              WHERE enumeration_type_id = :enumerationTypeId
                AND (:label IS NULL OR lower(label) LIKE concat('%', lower(:label), '%'))
                AND (:description IS NULL OR lower(description) LIKE concat('%', lower(:description), '%'))
              ORDER BY idx DESC NULLS LAST, label DESC NULLS LAST''')
    abstract List<EnumerationValue> readAllByEnumerationTypeIdAndFiltersDesc(UUID enumerationTypeId, @Nullable String label, @Nullable String description)

    @Query('''SELECT *
              FROM datamodel.enumeration_value
              WHERE enumeration_type_id = :enumerationTypeId
                AND (:label IS NULL OR lower(label) LIKE concat('%', lower(:label), '%'))
                AND (:description IS NULL OR lower(description) LIKE concat('%', lower(:description), '%'))
              ORDER BY idx ASC NULLS FIRST, label ASC NULLS FIRST
              LIMIT :max OFFSET :offset''')
    abstract List<EnumerationValue> readAllByEnumerationTypeIdAndFiltersAsc(UUID enumerationTypeId, @Nullable String label, @Nullable String description,
                                                                            Integer max, Integer offset)

    @Query('''SELECT *
              FROM datamodel.enumeration_value
              WHERE enumeration_type_id = :enumerationTypeId
                AND (:label IS NULL OR lower(label) LIKE concat('%', lower(:label), '%'))
                AND (:description IS NULL OR lower(description) LIKE concat('%', lower(:description), '%'))
              ORDER BY idx DESC NULLS LAST, label DESC NULLS LAST
              LIMIT :max OFFSET :offset''')
    abstract List<EnumerationValue> readAllByEnumerationTypeIdAndFiltersDesc(UUID enumerationTypeId, @Nullable String label, @Nullable String description,
                                                                             Integer max, Integer offset)

    abstract Long deleteByEnumerationTypeId(UUID dataTypeId)

    //    @Override
    Long deleteByOwnerId(UUID ownerId) {
        deleteByEnumerationTypeId(ownerId)
    }

    @Override
    Class getDomainClass() {
        EnumerationValue
    }


    abstract List<EnumerationValue> readAllByEnumerationTypeId(UUID enumerationTypeId)

    Boolean handlesPathPrefix(final String pathPrefix) {
        'ev'.equalsIgnoreCase(pathPrefix)
    }
}
