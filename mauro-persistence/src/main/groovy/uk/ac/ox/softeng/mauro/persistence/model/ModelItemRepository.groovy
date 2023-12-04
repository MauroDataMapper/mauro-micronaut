package uk.ac.ox.softeng.mauro.persistence.model

import uk.ac.ox.softeng.mauro.domain.model.AdministeredItem
import uk.ac.ox.softeng.mauro.domain.model.ModelItem

import groovy.transform.CompileStatic
import io.micronaut.data.repository.reactive.ReactorPageableRepository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@CompileStatic
abstract class ModelItemRepository<I extends ModelItem> extends AdministeredItemRepository<I> implements ReactorPageableRepository<I, UUID> {

    Flux<I> findAllByParent(AdministeredItem item) {
        // Should be implemented by override with joins, possibly using a DTO
        throw new UnsupportedOperationException('Method should be implemented')
    }

    abstract Mono<Long> deleteByOwnerId(UUID ownerId)
}