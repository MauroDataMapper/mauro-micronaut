package uk.ac.ox.softeng.mauro.persistence.folder

import groovy.transform.CompileStatic
import io.micronaut.core.annotation.Nullable
import io.micronaut.data.jdbc.annotation.JdbcRepository
import io.micronaut.data.model.query.builder.sql.Dialect
import jakarta.inject.Inject
import uk.ac.ox.softeng.mauro.domain.folder.Folder
import uk.ac.ox.softeng.mauro.persistence.folder.dto.FolderDTORepository
import uk.ac.ox.softeng.mauro.persistence.model.ModelRepository

@CompileStatic
@JdbcRepository(dialect = Dialect.POSTGRES)
abstract class FolderRepository implements ModelRepository<Folder> {

    @Inject
    FolderDTORepository folderDTORepository

    @Nullable
    Folder findById(UUID id) {
        folderDTORepository.findById(id) as Folder
    }

    @Nullable
    abstract List<Folder> readAllByParentFolder(Folder folder)

    @Override
    @Nullable
    List<Folder> readAllByFolder(Folder folder) {
        readAllByParentFolder(folder)
    }

    @Override
    Class getDomainClass() {
        Folder
    }

    @Nullable
    @Override
    List<Folder> findAllByFolderId(UUID folderId){
        findById(folderId) as List<Folder>
    }
}