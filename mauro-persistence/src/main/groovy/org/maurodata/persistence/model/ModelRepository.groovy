package org.maurodata.persistence.model

import jakarta.inject.Inject
import org.maurodata.domain.model.version.ModelVersion

import groovy.transform.CompileStatic
import io.micronaut.core.annotation.Nullable
import org.maurodata.domain.folder.Folder
import org.maurodata.domain.model.AdministeredItem
import org.maurodata.domain.model.Model
import org.maurodata.persistence.ContentsService

@CompileStatic
trait ModelRepository<M extends Model> implements AdministeredItemRepository<M> {

    ContentsService contentsService

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
    abstract List<M> readAllByFolderIdIn(Collection<UUID> folderIds)


    @Nullable
    abstract List<M> readAllByFinalisedTrue()

    @Nullable
    abstract M readByLabelAndModelVersion(String label, ModelVersion modelVersion)

    abstract Boolean handles(String domainType)

    M loadWithContent(UUID id) {
        M model = readById(id)
        (M) contentsService.loadWithContent (model)
        model
    }



}
