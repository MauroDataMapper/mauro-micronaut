package org.maurodata.persistence.folder.dto

import groovy.transform.CompileStatic
import io.micronaut.core.annotation.Nullable
import io.micronaut.data.annotation.Join
import io.micronaut.data.jdbc.annotation.JdbcRepository
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.repository.GenericRepository

@CompileStatic
@JdbcRepository(dialect = Dialect.POSTGRES)
abstract class FolderDTORepository implements GenericRepository<FolderDTO, UUID> {

    @Join(value = 'authority', type = Join.Type.LEFT_FETCH)
    @Join(value = 'childFolders', type = Join.Type.LEFT_FETCH)
    @Join(value = 'catalogueUser', type = Join.Type.LEFT_FETCH)
    @Nullable
    abstract FolderDTO findById(UUID id)
}
