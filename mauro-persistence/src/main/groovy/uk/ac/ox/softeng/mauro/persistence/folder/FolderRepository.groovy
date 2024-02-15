package uk.ac.ox.softeng.mauro.persistence.folder

import groovy.transform.CompileStatic
import io.micronaut.data.annotation.Join
import io.micronaut.data.jdbc.annotation.JdbcRepository
import io.micronaut.data.model.query.builder.sql.Dialect
import uk.ac.ox.softeng.mauro.domain.folder.Folder
import uk.ac.ox.softeng.mauro.persistence.model.ModelRepository

@CompileStatic
@JdbcRepository(dialect = Dialect.POSTGRES)
abstract class FolderRepository implements ModelRepository<Folder> {

    @Join(value = 'childFolders', type = Join.Type.LEFT_FETCH)
    abstract Folder findById(UUID id)

    abstract List<Folder> readAllByParentFolder(Folder folder)

    @Override
    List<Folder> readAllByFolder(Folder folder) {
        readAllByParentFolder(folder)
    }

    @Override
    Class getDomainClass() {
        Folder
    }
}