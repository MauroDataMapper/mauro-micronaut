package uk.ac.ox.softeng.mauro.persistence.datamodel

import uk.ac.ox.softeng.mauro.domain.datamodel.DataClass
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

abstract class DataClassRepository implements ReactorPageableRepository<DataClass, UUID>, ModelItemRepository<DataClass> {
    abstract Mono<DataClass> findByDataModelIdAndId(UUID dataModelId, UUID id)

    abstract Mono<DataClass> readByDataModelIdAndId(UUID dataModelId, UUID id)

    abstract Mono<Boolean> existsByDataModelIdAndId(UUID dataModelId, UUID id)

    abstract Mono<Long> deleteByDataModelId(UUID dataModelId)

    abstract Flux<DataClass> readAllByDataModel(DataModel dataModel)

    abstract Flux<DataClass> readAllByDataModelIdAndParentDataClassId(UUID dataModelId, UUID parentDataClassId)

    abstract Mono<DataClass> save(@Valid @NonNull DataClass item)

    @Override
    Flux<DataClass> readAllByParent(AdministeredItem parent) {
        readAllByDataModel((DataModel) parent)
    }

    @Override
    Mono<Long> deleteByOwnerId(UUID ownerId) {
        deleteByDataModelId(ownerId)
    }


    @Override
    Boolean handles(Class clazz) {
        clazz == DataClass
    }

}
