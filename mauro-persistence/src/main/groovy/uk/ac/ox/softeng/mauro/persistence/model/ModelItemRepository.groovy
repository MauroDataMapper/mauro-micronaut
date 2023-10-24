package uk.ac.ox.softeng.mauro.persistence.model

import uk.ac.ox.softeng.mauro.domain.model.ModelItem

import io.micronaut.core.annotation.NonNull
import io.micronaut.data.repository.reactive.ReactorPageableRepository
import jakarta.validation.Valid
import reactor.core.publisher.Mono

trait ModelItemRepository<I extends ModelItem> implements ReactorPageableRepository<I, UUID>, AdministeredItemRepository<I> {

    abstract Mono<Long> deleteByOwnerId(UUID ownerId)

}