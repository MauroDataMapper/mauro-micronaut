package uk.ac.ox.softeng.mauro.model

import reactor.core.publisher.Mono

trait ModelRepository<M extends Model> {

    abstract Mono<M> findById(UUID id)

    abstract Mono<M> readById(UUID id)

    abstract Boolean handles(Class clazz)

    abstract Boolean handles(String domainType)
}
