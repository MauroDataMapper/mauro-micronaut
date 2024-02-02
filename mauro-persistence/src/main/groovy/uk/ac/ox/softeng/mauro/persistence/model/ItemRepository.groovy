package uk.ac.ox.softeng.mauro.persistence.model

import uk.ac.ox.softeng.mauro.domain.model.Item

import groovy.transform.CompileStatic
import io.micronaut.core.annotation.NonNull
import io.micronaut.data.repository.GenericRepository
import io.micronaut.data.repository.reactive.ReactorPageableRepository
import jakarta.validation.Valid
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@CompileStatic
trait ItemRepository<I extends Item> implements GenericRepository<I, UUID> {

    // Should be implemented by override with facet joins, possibly using a DTO
    abstract Mono<I> findById(UUID id)

    abstract Mono<I> readById(UUID id)

    abstract Mono<I> save(@Valid @NonNull I item)

    abstract Flux<I> saveAll(@Valid @NonNull Iterable<I> items)

    abstract Mono<I> update(@Valid @NonNull I item)

    abstract Mono<Long> delete(@NonNull I item)

    abstract Class<I> getDomainClass()

    Boolean handles(Class clazz) {
        clazz == domainClass
    }
}
