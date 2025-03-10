package uk.ac.ox.softeng.mauro.persistence.datamodel

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import io.micronaut.core.annotation.Nullable
import io.micronaut.data.jdbc.annotation.JdbcRepository
import io.micronaut.data.model.query.builder.sql.Dialect
import jakarta.inject.Inject
import uk.ac.ox.softeng.mauro.domain.datamodel.DataType
import uk.ac.ox.softeng.mauro.domain.datamodel.EnumerationValue
import uk.ac.ox.softeng.mauro.domain.model.AdministeredItem
import uk.ac.ox.softeng.mauro.persistence.datamodel.dto.EnumerationValueDTORepository
import uk.ac.ox.softeng.mauro.persistence.model.ModelItemRepository

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
    List<EnumerationValue> findAllByEnumerationType(DataType dataType) {
        enumerationValueDTORepository.findAllByEnumerationType(dataType) as List<EnumerationValue>
    }

    @Override
    @Nullable
    List<EnumerationValue> findAllByParent(AdministeredItem parent) {
        findAllByEnumerationType((DataType) parent)
    }

    @Nullable
    abstract Set<EnumerationValue> readAllByEnumerationTypeIn(Collection<DataType> dataTypes)

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


}
