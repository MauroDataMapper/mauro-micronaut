package uk.ac.ox.softeng.mauro.model

import reactor.core.publisher.Mono

trait ModelRepository<M extends Model> {

    abstract Mono<M> findById(UUID id)

    abstract Mono<M> readById(UUID id)

    Boolean handles(Class clazz) {
        throw new UnsupportedOperationException('To be overridden')
    }

    Boolean handles(String domainType) {
        throw new UnsupportedOperationException('To be overridden')
    }
}
