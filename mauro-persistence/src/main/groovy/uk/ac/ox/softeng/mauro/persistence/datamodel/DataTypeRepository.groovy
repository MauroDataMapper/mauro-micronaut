package uk.ac.ox.softeng.mauro.persistence.datamodel

import uk.ac.ox.softeng.mauro.domain.datamodel.DataModel
import uk.ac.ox.softeng.mauro.domain.datamodel.DataType

import uk.ac.ox.softeng.mauro.domain.model.AdministeredItem
import uk.ac.ox.softeng.mauro.persistence.model.ModelItemRepository

import io.micronaut.core.annotation.NonNull
import io.micronaut.data.annotation.Join
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.r2dbc.annotation.R2dbcRepository

import io.micronaut.data.repository.reactive.ReactorPageableRepository
import jakarta.validation.Valid
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono


@R2dbcRepository(dialect = Dialect.POSTGRES)
abstract class DataTypeRepository implements ReactorPageableRepository<DataType, UUID>, ModelItemRepository<DataType> {

    @Join(value = 'enumerationValues', type = Join.Type.LEFT_FETCH)
    abstract Mono<DataType> findByDataModelIdAndId(UUID dataModelId, UUID id)

    @Join(value = 'enumerationValues', type = Join.Type.LEFT_FETCH)
    abstract Mono<DataType> readByDataModelIdAndId(UUID dataModelId, UUID id)

    abstract Mono<Boolean> existsByDataModelIdAndId(UUID dataModelId, UUID id)

    abstract Mono<Long> deleteByDataModelId(UUID dataModelId)

    abstract Flux<DataType> readAllByDataModel(DataModel dataModel)

    abstract Mono<DataType> save(@Valid @NonNull DataType item)

    @Override
    @Join(value = 'enumerationValues', type = Join.Type.LEFT_FETCH)
    Mono<DataType> findByParentIdAndId(UUID parentId, UUID id) {
        findByDataModelIdAndId(parentId, id)
    }

    @Override
    @Join(value = 'enumerationValues', type = Join.Type.LEFT_FETCH)
    Mono<DataType> readByParentIdAndId(UUID parentId, UUID id) {
        readByDataModelIdAndId(parentId, id)
    }

    @Override
    Flux<DataType> readAllByParent(AdministeredItem parent) {
        readAllByDataModel((DataModel) parent)
    }

    @Override
    Mono<Long> deleteByOwnerId(UUID ownerId) {
        deleteByDataModelId(ownerId)
    }


    @Override
    Boolean handles(Class clazz) {
        clazz == DataType
    }

    @Override
    Boolean handles(String domainType) {
        domainType.toLowerCase() in ['datatype', 'datatypes']
    }

}
