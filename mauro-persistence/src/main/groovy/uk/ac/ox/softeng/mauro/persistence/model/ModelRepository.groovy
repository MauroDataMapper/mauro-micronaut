package uk.ac.ox.softeng.mauro.persistence.model

import groovy.transform.CompileStatic
import io.micronaut.core.annotation.Nullable
import uk.ac.ox.softeng.mauro.domain.folder.Folder
import uk.ac.ox.softeng.mauro.domain.model.AdministeredItem
import uk.ac.ox.softeng.mauro.domain.model.Model

@CompileStatic
trait ModelRepository<M extends Model> implements AdministeredItemRepository<M> {

    @Nullable
    abstract List<M> readAllByFolder(Folder folder)

    @Nullable
    abstract List<M> readAll()

    @Override
    @Nullable
    List<M> readAllByParent(AdministeredItem item) {
        readAllByFolder((Folder) item)
    }
    @Nullable
    abstract List<M> findAllByFolderId(UUID folderId)
}
