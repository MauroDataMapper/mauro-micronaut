package uk.ac.ox.softeng.mauro.persistence.folder

import uk.ac.ox.softeng.mauro.domain.model.AdministeredItem

import groovy.transform.CompileStatic
import io.micronaut.core.annotation.NonNull
import io.micronaut.data.annotation.Join
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.r2dbc.annotation.R2dbcRepository
import io.micronaut.data.repository.reactive.ReactorPageableRepository
import jakarta.validation.Valid
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import uk.ac.ox.softeng.mauro.domain.folder.Folder
import uk.ac.ox.softeng.mauro.persistence.model.ModelRepository

@CompileStatic
@R2dbcRepository(dialect = Dialect.POSTGRES)
abstract class FolderRepository implements ReactorPageableRepository<Folder, UUID>, ModelRepository<Folder> {

    @Join(value = 'childFolders', type = Join.Type.LEFT_FETCH)
    abstract Mono<Folder> findById(UUID id)

    @Join(value = 'childFolders', type = Join.Type.LEFT_FETCH)
    abstract Mono<Folder> findByParentFolderIdAndId(UUID parentFolderId, UUID id)

    @Join(value = 'childFolders', type = Join.Type.LEFT_FETCH)
    abstract Mono<Folder> findByIdAndVersion(UUID id, Integer version)

    abstract Mono<Folder> readById(UUID id)

    abstract Mono<Folder> readByIdAndVersion(UUID id, Integer version)

    abstract Mono<Folder> readByParentFolderIdAndId(UUID parentFolderId, UUID id)

    abstract Flux<Folder> readAllByParentFolder(Folder folder)

    @Override
    Mono<Folder> findByParentIdAndId(UUID parentId, UUID id) {
        findByParentFolderIdAndId(parentId, id)
    }

    @Override
    Mono<Folder> readByParentIdAndId(UUID parentId, UUID id) {
        readByParentFolderIdAndId(parentId, id)
    }

    @Override
    Flux<Folder> readAllByFolder(Folder folder) {
        readAllByParentFolder(folder)
    }

    @Override
    Flux<Folder> readAllByParent(AdministeredItem parent) {
        readAllByParentFolder((Folder) parent)
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