package uk.ac.ox.softeng.mauro.persistence.model

import uk.ac.ox.softeng.mauro.domain.model.Item

import groovy.transform.CompileStatic
import io.micronaut.core.annotation.NonNull
import io.micronaut.data.repository.reactive.ReactorPageableRepository
import jakarta.validation.Valid
import reactor.core.publisher.Mono

@CompileStatic
trait ItemRepository<I extends Item> implements ReactorPageableRepository<I, UUID> {

    Mono<I> findById(UUID id) {
        // Should be implemented by override with joins, possibly using a DTO
        throw new UnsupportedOperationException('Method should be implemented')
    }

    abstract Mono<I> readById(UUID id)

    abstract Mono<I> save(@Valid @NonNull I item)

    abstract Mono<I> update(@Valid @NonNull I item)

    abstract Boolean handles(Class clazz)
}
