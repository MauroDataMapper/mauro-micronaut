package org.maurodata.persistence.folder.dto

import org.maurodata.domain.folder.Folder

import groovy.transform.CompileStatic
import io.micronaut.core.annotation.Nullable
import io.micronaut.data.annotation.Join
import io.micronaut.data.annotation.Query
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

    @Join(value = 'authority', type = Join.Type.LEFT_FETCH)
    @Join(value = 'childFolders', type = Join.Type.LEFT_FETCH)
    @Join(value = 'catalogueUser', type = Join.Type.LEFT_FETCH)
    @Nullable
    @Query('SELECT * FROM core.folder WHERE parent_folder_id = :item AND label = :pathIdentifier')
    abstract List<FolderDTO> findAllByParentAndPathIdentifier(UUID item, String pathIdentifier)

    @Nullable
    abstract List<FolderDTO> findAllByParentFolderId(UUID item)



    @Nullable
    @Query('SELECT * FROM core.folder WHERE label = :label')
    abstract List<Folder> findAllByLabel(String label)
}

