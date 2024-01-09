package uk.ac.ox.softeng.mauro.persistence.datamodel

import uk.ac.ox.softeng.mauro.domain.datamodel.DataClass
import uk.ac.ox.softeng.mauro.domain.datamodel.DataElement
import uk.ac.ox.softeng.mauro.domain.datamodel.DataModel
import uk.ac.ox.softeng.mauro.domain.model.AdministeredItem
import uk.ac.ox.softeng.mauro.persistence.model.ModelItemRepository

import io.micronaut.core.annotation.NonNull
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.r2dbc.annotation.R2dbcRepository
import io.micronaut.data.repository.reactive.ReactorPageableRepository
import jakarta.validation.Valid
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@R2dbcRepository(dialect = Dialect.POSTGRES)

abstract class DataElementRepository implements ReactorPageableRepository<DataElement, UUID>, ModelItemRepository<DataElement> {
    abstract Mono<DataElement> findByDataClassIdAndId(UUID dataClassId, UUID id)

    abstract Mono<DataElement> readByDataClassIdAndId(UUID dataClassId, UUID id)

    abstract Mono<Boolean> existsByDataClassIdAndId(UUID dataClassId, UUID id)

    abstract Mono<Long> deleteByDataClassId(UUID dataClassId)

    abstract Flux<DataElement> readAllByDataClass(DataClass dataClass)

    abstract Mono<DataElement> save(@Valid @NonNull DataElement item)

    @Override
    Flux<DataElement> readAllByParent(AdministeredItem parent) {
        readAllByDataClass((DataClass) parent)
    }

    @Override
    Mono<Long> deleteByOwnerId(UUID ownerId) {
        deleteByDataClassId(ownerId)
    }


    @Override
    Boolean handles(Class clazz) {
        clazz == DataElement
    }

}
