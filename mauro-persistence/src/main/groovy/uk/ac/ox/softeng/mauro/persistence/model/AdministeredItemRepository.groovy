package uk.ac.ox.softeng.mauro.persistence.model

import uk.ac.ox.softeng.mauro.domain.model.AdministeredItem

import groovy.transform.CompileStatic
import io.micronaut.core.annotation.NonNull
import io.micronaut.data.repository.reactive.ReactorPageableRepository
import jakarta.validation.Valid
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@CompileStatic
trait AdministeredItemRepository<I extends AdministeredItem> implements ReactorPageableRepository<I, UUID> {

    abstract Boolean handles(Class clazz)

    abstract Boolean handles(String domainType)

    abstract Mono<I> findById(UUID id)

    abstract Mono<I> findByParentIdAndId(UUID parentId, UUID id)

    abstract Mono<I> readById(UUID id)

    abstract Mono<I> readByParentIdAndId(UUID parentId, UUID id)

    abstract Mono<I> save(@Valid @NonNull I item)

    abstract Mono<I> update(@Valid @NonNull I item)

    abstract Flux<I> readAllByParent(AdministeredItem item)
}