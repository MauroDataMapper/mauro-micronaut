package uk.ac.ox.softeng.mauro.persistence.model

import groovy.transform.CompileStatic
import uk.ac.ox.softeng.mauro.domain.folder.Folder
import uk.ac.ox.softeng.mauro.domain.model.AdministeredItem
import uk.ac.ox.softeng.mauro.domain.model.Model

@CompileStatic
trait ModelRepository<M extends Model> implements AdministeredItemRepository<M> {

    abstract List<M> readAllByFolder(Folder folder)

    abstract List<M> readAll()

    @Override
    List<M> readAllByParent(AdministeredItem item) {
        readAllByFolder((Folder) item)
    }
}
