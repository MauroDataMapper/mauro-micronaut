package org.maurodata.service.path

import groovy.transform.CompileStatic
import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.maurodata.domain.model.Model
import org.maurodata.domain.model.Path
import org.maurodata.domain.security.Role
import org.maurodata.persistence.cache.ModelCacheableRepository
import org.maurodata.persistence.model.PathRepository
import org.maurodata.persistence.service.RepositoryService
import org.maurodata.security.AccessControlService

/** Resolves visible Model candidates for a Path root without resolving the rest of the path. */
@CompileStatic
@Singleton
class VisiblePathModelResolver {
    @Inject PathRepository pathRepository
    @Inject RepositoryService repositoryService
    @Inject AccessControlService accessControlService

    PathModelCandidates findVisibleModelCandidatesForPathRoot(Path.PathNode root) {
        Map<UUID, Model> finalisedModels = [:]
        Map<UUID, Model> draftModels = [:]

        repositoryService.modelCacheableRepositories.each { ModelCacheableRepository repo ->
            (repo.readAll() as List<Model>).each { Model model ->
                pathRepository.readParentItems(model)
                model.updatePath()
                Model versionOwner = model.owner
                if (!versionOwner) return
                pathRepository.readParentItems(versionOwner)
                versionOwner.updatePath()
                Path.PathNode modelRoot = versionOwner.path?.modelRootNode
                if (!root.hasSamePrefixAndIdentifier(modelRoot) || !accessControlService.canDoRole(Role.READER, versionOwner)) return

                if (versionOwner.matchesFinalisedPathModelIdentifier(root.modelIdentifier)) {
                    finalisedModels[versionOwner.id] = versionOwner
                } else if (versionOwner.matchesDraftPathModelIdentifier(root.modelIdentifier)) {
                    draftModels[versionOwner.id] = versionOwner
                }
            }
        }

        new PathModelCandidates(finalisedModels.values() as List<Model>, draftModels.values() as List<Model>)
    }
}
