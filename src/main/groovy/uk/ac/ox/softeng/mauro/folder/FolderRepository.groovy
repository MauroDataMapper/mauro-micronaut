package uk.ac.ox.softeng.mauro.folder

import uk.ac.ox.softeng.mauro.model.ModelRepository

import io.micronaut.core.annotation.NonNull
import io.micronaut.data.annotation.Join
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.r2dbc.annotation.R2dbcRepository
import io.micronaut.data.repository.reactive.ReactorPageableRepository
import jakarta.validation.Valid
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@R2dbcRepository(dialect = Dialect.POSTGRES)
abstract class FolderRepository implements ReactorPageableRepository<Folder, UUID>, ModelRepository<Folder> {

    @Join(value = 'childFolders', type = Join.Type.LEFT_FETCH)
    abstract Mono<Folder> findById(UUID id)

    @Join(value = 'childFolders', type = Join.Type.LEFT_FETCH)
    abstract Mono<Folder> findByIdAndVersion(UUID id, Integer version)

    abstract Mono<Folder> readById(UUID id)

    abstract Mono<Folder> readByIdAndVersion(UUID id, Integer version)

    abstract Flux<Folder> readAllByParentFolder(Folder folder)

    abstract Mono<Folder> save(@Valid @NonNull Folder folder)

    abstract Mono<Folder> update(@Valid @NonNull Folder folder)

    Flux<Folder> readAllByFolder(Folder folder) {
        readAllByParentFolder(folder)
    }

    @Override
    Boolean handles(Class clazz) {
        clazz == Folder
    }

    @Override
    Boolean handles(String domainType) {
        domainType.toLowerCase() in ['folder', 'folders']
    }
}