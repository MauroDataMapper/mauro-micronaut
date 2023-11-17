package uk.ac.ox.softeng.mauro.persistence.datamodel

import uk.ac.ox.softeng.mauro.domain.datamodel.DataModel
import uk.ac.ox.softeng.mauro.domain.datamodel.DataType
import uk.ac.ox.softeng.mauro.domain.datamodel.EnumerationValue
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
abstract class EnumerationValueRepository implements ReactorPageableRepository<EnumerationValue, UUID>, ModelItemRepository<EnumerationValue> {

    abstract Mono<EnumerationValue> findByEnumerationTypeIdAndId(UUID enumerationTypeId, UUID id)

    abstract Mono<EnumerationValue> readByEnumerationTypeIdAndId(UUID enumerationTypeId, UUID id)

    abstract Mono<Boolean> existsByEnumerationTypeIdAndId(UUID enumerationTypeId, UUID id)

    abstract Mono<Long> deleteByEnumerationTypeId(UUID enumerationTypeId)

    abstract Flux<EnumerationValue> readAllByEnumerationType(DataType dataType)

    abstract Mono<EnumerationValue> save(@Valid @NonNull EnumerationValue item)

    @Override
    Mono<EnumerationValue> findByParentIdAndId(UUID parentId, UUID id) {
        findByEnumerationTypeIdAndId(parentId, id)
    }

    @Override
    Mono<EnumerationValue> readByParentIdAndId(UUID parentId, UUID id) {
        readByEnumerationTypeIdAndId(parentId, id)
    }

    @Override
    Flux<EnumerationValue> readAllByParent(AdministeredItem parent) {
        readAllByEnumerationType((DataType) parent)
    }

    @Override
    Mono<Long> deleteByOwnerId(UUID ownerId) {
        deleteByEnumerationTypeId(ownerId)
    }


    @Override
    Boolean handles(Class clazz) {
        clazz == EnumerationValue
    }

    @Override
    Boolean handles(String domainType) {
        domainType.toLowerCase() in ['enumerationvalue', 'enumerationvalues']
    }

}
