package uk.ac.ox.softeng.mauro.persistence.model

import uk.ac.ox.softeng.mauro.domain.model.AdministeredItem

import groovy.transform.CompileStatic
import io.micronaut.core.annotation.NonNull
import io.micronaut.data.repository.reactive.ReactorPageableRepository
import jakarta.validation.Valid
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@CompileStatic
trait AdministeredItemRepository<I extends AdministeredItem> implements ItemRepository<I> {

    Flux<I> findAllByParent(AdministeredItem item) {
        // Should be implemented by override with joins, possibly using a DTO
        throw new UnsupportedOperationException('Method should be implemented')
    }

    abstract Flux<I> readAllByParent(AdministeredItem item)
}