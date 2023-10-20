package uk.ac.ox.softeng.mauro.persistence.model

import uk.ac.ox.softeng.mauro.domain.model.AdministeredItem

import io.micronaut.data.repository.reactive.ReactorPageableRepository
import reactor.core.publisher.Mono

trait AdministeredItemRepository<I extends AdministeredItem> implements ReactorPageableRepository<I, UUID> {

    abstract Boolean handles(Class clazz)

    abstract Boolean handles(String domainType)

    abstract Mono<I> readById(UUID id)

}