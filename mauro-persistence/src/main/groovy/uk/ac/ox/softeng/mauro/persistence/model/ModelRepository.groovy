package uk.ac.ox.softeng.mauro.persistence.model

import uk.ac.ox.softeng.mauro.domain.folder.Folder
import uk.ac.ox.softeng.mauro.domain.model.Model

import groovy.transform.CompileStatic
import io.micronaut.data.repository.reactive.ReactorPageableRepository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@CompileStatic
trait ModelRepository<M extends Model> implements ReactorPageableRepository<M, UUID>, AdministeredItemRepository<M> {

    Mono<M> findById(UUID id) {
        throw new UnsupportedOperationException('Method should be overridden with appropriate joins defined')
    }

    abstract Flux<M> readAllByFolder(Folder folder)

    abstract Flux<M> readAll()
}
