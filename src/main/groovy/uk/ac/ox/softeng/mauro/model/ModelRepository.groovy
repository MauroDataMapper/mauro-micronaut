package uk.ac.ox.softeng.mauro.model

import io.micronaut.data.repository.reactive.ReactorPageableRepository
import reactor.core.publisher.Mono

trait ModelRepository<M extends Model> implements ReactorPageableRepository<M, UUID> {

    abstract Mono<M> findById(UUID id)

    abstract Mono<M> readById(UUID id)

    abstract Boolean handles(Class clazz)

    abstract Boolean handles(String domainType)
}
