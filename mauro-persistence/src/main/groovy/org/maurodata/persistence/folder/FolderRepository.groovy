package org.maurodata.persistence.folder

import groovy.transform.CompileStatic
import io.micronaut.core.annotation.Nullable
import io.micronaut.data.jdbc.annotation.JdbcRepository
import io.micronaut.data.model.query.builder.sql.Dialect
import jakarta.inject.Inject
import org.maurodata.domain.folder.Folder
import org.maurodata.domain.model.AdministeredItem
import org.maurodata.persistence.folder.dto.FolderDTORepository
import org.maurodata.persistence.model.ModelRepository

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
    List<Folder> findAllByParentAndPathIdentifier(UUID item, String pathIdentifier) {
        folderDTORepository.findAllByParentAndPathIdentifier(item, pathIdentifier)
    }

    @Nullable
    abstract List<Folder> readAllByParentFolder(Folder folder)

    @Nullable
    abstract List<Folder> readAllByParentFolderIsNull()

    @Override
    @Nullable
    List<Folder> readAllByFolder(Folder folder) {
        readAllByParentFolder(folder)
    }

    @Nullable
    List<Folder> readAllRootFolders() {
        readAllByParentFolderIsNull()
    }

    @Override
    Class getDomainClass() {
        Folder
    }

    @Nullable
    @Override
    List<Folder> findAllByFolderId(UUID folderId) {
        findAllByParent(folderId as AdministeredItem)
    }

    @Override
    Boolean handles(String domainType) {
        return domainType != null && domainType.toLowerCase() in ['folder', 'folders', 'versionedfolder', 'versionedfolders']
    }

    Boolean handlesPathPrefix(final String pathPrefix) {
        'fo'.equalsIgnoreCase(pathPrefix) || 'vf'.equalsIgnoreCase(pathPrefix)
    }
}