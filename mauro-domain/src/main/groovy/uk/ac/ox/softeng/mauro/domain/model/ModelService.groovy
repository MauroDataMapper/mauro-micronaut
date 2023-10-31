package uk.ac.ox.softeng.mauro.domain.model

import uk.ac.ox.softeng.mauro.exception.MauroApplicationException

import groovy.transform.CompileStatic
import uk.ac.ox.softeng.mauro.domain.model.version.ModelVersion
import uk.ac.ox.softeng.mauro.domain.model.version.VersionChangeType
import uk.ac.ox.softeng.mauro.domain.tree.TreeItem

import io.micronaut.core.annotation.Nullable

import java.time.OffsetDateTime

/**
 * A Service class that provides utility functions for working with data models.
 */
@CompileStatic
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

    M finaliseModel(M model, @Nullable ModelVersion requestedModelVersion, @Nullable VersionChangeType versionChangeType, @Nullable String versionTag) {
        if (!requestedModelVersion && !versionChangeType) throw new IllegalArgumentException('A version or versionChangeType must be specified to finalise a Model')
        if (model.branchName != 'main') throw new MauroApplicationException("Cannot finalise Model [$model.label] as it is not on branch 'main'")
        if (model.finalised) throw new MauroApplicationException("Cannot finalise Model [$model.label] as it is already finalised")

        model.finalised = true
        model.dateFinalised = OffsetDateTime.now()

        model.modelVersion = requestedModelVersion ?: (model.modelVersion ?: new ModelVersion([:])).nextVersion(versionChangeType)
        model.modelVersionTag = versionTag

        model
    }
}