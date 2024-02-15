package uk.ac.ox.softeng.mauro.persistence.datamodel

import uk.ac.ox.softeng.mauro.domain.datamodel.DataModel
import uk.ac.ox.softeng.mauro.domain.folder.Folder
import uk.ac.ox.softeng.mauro.domain.model.AdministeredItem
import uk.ac.ox.softeng.mauro.persistence.model.ModelRepository

import groovy.transform.CompileStatic
import io.micronaut.data.annotation.Join
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.r2dbc.annotation.R2dbcRepository
import io.micronaut.data.repository.reactive.ReactorPageableRepository
import jakarta.inject.Inject
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@CompileStatic
@R2dbcRepository(dialect = Dialect.POSTGRES)
abstract class DataModelRepository implements ReactorPageableRepository<DataModel, UUID>, ModelRepository<DataModel> {

    @Inject
    DataModelDTORepository dataModelDtoRepository

    @Join(value = 'dataTypes', type = Join.Type.LEFT_FETCH)
    abstract Mono<DataModel> findById(UUID id)

    @Join(value = 'dataTypes', type = Join.Type.LEFT_FETCH)
    abstract Mono<DataModel> findByFolderIdAndId(UUID folderId, UUID id)

    abstract Mono<DataModel> readById(UUID id)

    abstract Mono<DataModel> readByFolderIdAndId(UUID folderId, UUID id)

    abstract Flux<DataModel> readAllByFolder(Folder folder)

    @Override
    Flux<DataModel> readAllByParent(AdministeredItem parent) {
        readAllByFolder((Folder) parent)
    }

    @Override
    Boolean handles(Class clazz) {
        clazz == DataModel
    }
}
