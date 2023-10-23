package uk.ac.ox.softeng.mauro.persistence.model

import uk.ac.ox.softeng.mauro.domain.model.AdministeredItem

import io.micronaut.data.repository.reactive.ReactorPageableRepository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

trait AdministeredItemRepository<I extends AdministeredItem> implements ReactorPageableRepository<I, UUID> {

    abstract Boolean handles(Class clazz)

    abstract Boolean handles(String domainType)

    abstract Mono<I> findById(UUID id)

    abstract Mono<I> findByParentIdAndId(UUID parentId, UUID id)

    abstract Mono<I> readById(UUID id)

    abstract Mono<I> readByParentIdAndId(UUID parentId, UUID id)

    abstract Flux<I> readAllByParent(AdministeredItem item)
}