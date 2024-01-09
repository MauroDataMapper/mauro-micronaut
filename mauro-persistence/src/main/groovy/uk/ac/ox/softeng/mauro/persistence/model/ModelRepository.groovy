package uk.ac.ox.softeng.mauro.persistence.model

import uk.ac.ox.softeng.mauro.domain.folder.Folder
import uk.ac.ox.softeng.mauro.domain.model.AdministeredItem
import uk.ac.ox.softeng.mauro.domain.model.Model

import groovy.transform.CompileStatic
import io.micronaut.data.repository.reactive.ReactorPageableRepository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@CompileStatic
trait ModelRepository<M extends Model> implements   AdministeredItemRepository<M> {

    abstract Flux<M> readAllByFolder(Folder folder)

    abstract Flux<M> readAll()

    @Override
    Flux<M> readAllByParent(AdministeredItem item) {
        readAllByFolder((Folder) item)
    }
}
