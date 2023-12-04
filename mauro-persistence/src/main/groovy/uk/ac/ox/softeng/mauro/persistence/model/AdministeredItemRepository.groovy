package uk.ac.ox.softeng.mauro.persistence.model

import uk.ac.ox.softeng.mauro.domain.model.AdministeredItem

import groovy.transform.CompileStatic
import io.micronaut.core.annotation.NonNull
import io.micronaut.data.repository.reactive.ReactorPageableRepository
import jakarta.validation.Valid
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@CompileStatic
abstract class AdministeredItemRepository<I extends AdministeredItem> extends ItemRepository<I> implements ReactorPageableRepository<I, UUID> {

    abstract Mono<I> readById(UUID id)

    abstract Flux<I> readAllByParent(AdministeredItem item)
}