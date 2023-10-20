package uk.ac.ox.softeng.mauro.persistence.model

import uk.ac.ox.softeng.mauro.domain.model.AdministeredItem
import uk.ac.ox.softeng.mauro.exception.MauroInternalException

import io.micronaut.core.annotation.NonNull
import io.micronaut.data.repository.reactive.ReactorPageableRepository
import io.micronaut.transaction.annotation.Transactional
import jakarta.inject.Inject
import jakarta.validation.Valid
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import uk.ac.ox.softeng.mauro.domain.folder.Folder
import uk.ac.ox.softeng.mauro.domain.model.Model

trait ModelRepository<M extends Model> implements ReactorPageableRepository<M, UUID>, AdministeredItemRepository<M> {

    Mono<M> findById(UUID id) {
        throw new UnsupportedOperationException('Method should be overridden with appropriate joins defined')
    }

    abstract Mono<M> readById(UUID id)

    abstract Flux<M> readAllByFolder(Folder folder)

    abstract Flux<M> readAll()

    Mono<M> save(@Valid @NonNull M model) {
        throw new UnsupportedOperationException('Method should be overridden with @Valid and @NonNull added')
    }

    Mono<M> update(@Valid @NonNull M model) {
        throw new UnsupportedOperationException('Method should be overridden with @Valid and @NonNull added')
    }
}
