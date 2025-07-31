package org.maurodata.persistence.model

import org.maurodata.domain.model.version.ModelVersion

import groovy.transform.CompileStatic
import io.micronaut.core.annotation.Nullable
import org.maurodata.domain.folder.Folder
import org.maurodata.domain.model.AdministeredItem
import org.maurodata.domain.model.Model

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

    @Nullable
    abstract List<M> readAllByFinalisedTrue()

    @Nullable
    abstract M readByLabelAndModelVersion(String label, ModelVersion modelVersion)

    abstract Boolean handles(String domainType)
}
