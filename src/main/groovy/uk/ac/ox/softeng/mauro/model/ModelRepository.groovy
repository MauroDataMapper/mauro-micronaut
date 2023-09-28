package uk.ac.ox.softeng.mauro.model

import uk.ac.ox.softeng.mauro.folder.Folder

import io.micronaut.core.annotation.NonNull
import io.micronaut.data.repository.reactive.ReactorPageableRepository
import jakarta.validation.Valid
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

import java.lang.reflect.Executable

trait ModelRepository<M extends Model> implements ReactorPageableRepository<M, UUID> {

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

    abstract Mono<Boolean> deleteWithContent(@NonNull M model)

    abstract Boolean handles(Class clazz)

    abstract Boolean handles(String domainType)
}
