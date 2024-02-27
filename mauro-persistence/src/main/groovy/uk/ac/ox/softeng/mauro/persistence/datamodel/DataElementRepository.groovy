package uk.ac.ox.softeng.mauro.persistence.datamodel

import uk.ac.ox.softeng.mauro.domain.datamodel.DataClass
import uk.ac.ox.softeng.mauro.domain.datamodel.DataElement
import uk.ac.ox.softeng.mauro.domain.datamodel.DataModel
import uk.ac.ox.softeng.mauro.domain.model.AdministeredItem
import uk.ac.ox.softeng.mauro.domain.terminology.Term
import uk.ac.ox.softeng.mauro.domain.terminology.Terminology
import uk.ac.ox.softeng.mauro.persistence.datamodel.dto.DataElementDTORepository
import uk.ac.ox.softeng.mauro.persistence.model.ModelItemRepository
import uk.ac.ox.softeng.mauro.persistence.terminology.dto.TermDTORepository

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import io.micronaut.core.annotation.NonNull
import io.micronaut.core.annotation.Nullable
import io.micronaut.data.annotation.Query
import io.micronaut.data.jdbc.annotation.JdbcRepository
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.repository.reactive.ReactorPageableRepository
import jakarta.inject.Inject
import jakarta.validation.Valid

@Slf4j
@CompileStatic
@JdbcRepository(dialect = Dialect.POSTGRES)
abstract class DataElementRepository implements ModelItemRepository<DataElement> {

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
    abstract List<DataElement> findAllByDataClassIn(Collection<DataClass> dataClasses)

    @Nullable
    abstract List<DataElement> readAllByDataClass(DataClass dataClass)

    @Override
    @Nullable
    List<DataElement> readAllByParent(AdministeredItem parent) {
        readAllByDataClass((DataClass) parent)
    }

    abstract Long deleteByDataClassId(UUID dataClassId)

    //    @Override
    Long deleteByOwnerId(UUID ownerId) {
        deleteByDataClassId(ownerId)
    }

    @Override
    Class getDomainClass() {
        DataElement
    }
}
