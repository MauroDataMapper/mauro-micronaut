package uk.ac.ox.softeng.mauro.domain.model

import uk.ac.ox.softeng.mauro.domain.model.version.ModelVersion
import uk.ac.ox.softeng.mauro.domain.model.version.VersionChangeType
import uk.ac.ox.softeng.mauro.domain.tree.TreeItem

import java.time.OffsetDateTime

abstract class ModelService<M extends Model> {

    abstract Boolean handles(Class clazz)
    abstract Boolean handles(String domainType)

    M updateDerived(M model) {
        updatePaths(model)
        model
    }

    M updatePaths(M model) {
        model.updatePath()
        model.getAllContents().each {AdministeredItem item ->
            item.updatePath()
        }
        model
    }

    M finaliseModel(M model, ModelVersion requestedModelVersion, VersionChangeType versionChangeType, String versionTag) {
        model.finalised = true
        model.dateFinalised = OffsetDateTime.now()
        model.modelVersion = requestedModelVersion ?: (model.modelVersion ?: new ModelVersion()).nextVersion(versionChangeType) //getNextModelVersion(model, requestedModelVersion, versionChangeType)
        model.modelVersionTag = versionTag

        model
    }
}
