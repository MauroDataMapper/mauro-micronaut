package org.maurodata.service.link

import groovy.transform.CompileStatic
import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.maurodata.domain.model.AdministeredItem
import org.maurodata.domain.model.Model
import org.maurodata.domain.model.Path
import org.maurodata.domain.security.Role
import org.maurodata.persistence.model.PathRepository
import org.maurodata.security.AccessControlService
import org.maurodata.service.path.PathModelCandidates
import org.maurodata.service.path.VisiblePathModelResolver

/** Expands and resolves link paths using the same catalogue and permission rules for every link kind. */
@CompileStatic
@Singleton
class ItemLinkPathResolver {
    @Inject PathRepository pathRepository
    @Inject VisiblePathModelResolver visiblePathModelResolver
    @Inject AccessControlService accessControlService

    ResolvedItemLinkPath resolve(AdministeredItem target, Path storedPath) {
        if (!storedPath || !storedPath.nodes) return new ResolvedItemLinkPath(path: storedPath)

        Path path = new Path(storedPath.toString())
        Model targetModel = versionOwningModel(target)
        if (!targetModel) return new ResolvedItemLinkPath(path: path)

        Path absolutePath = path
        Model sourceModel
        if (path.isAbsolute()) {
            Path.PathNode modelRoot = path.modelRootNode
            if (!modelRoot) return new ResolvedItemLinkPath(path: path)
            PathModelCandidates candidates = visiblePathModelResolver.findVisibleModelCandidatesForPathRoot(modelRoot)
            if (candidates.finalisedModels.size() != 1 || candidates.draftModels) {
                return new ResolvedItemLinkPath(path: path)
            }
            sourceModel = candidates.finalisedModels[0]
        } else {
            Path.PathNode modelRoot = targetModel.path?.modelRootNode
            if (!modelRoot) return new ResolvedItemLinkPath(path: path)
            absolutePath = new Path(([modelRoot] + path.nodes) as List<Path.PathNode>)
            sourceModel = targetModel
        }

        AdministeredItem source = pathRepository.findResourcesByPathFromRootResource(sourceModel, absolutePath)
        if (!source) return new ResolvedItemLinkPath(path: absolutePath)

        pathRepository.readParentItems(source)
        source.updatePath()
        if (!accessControlService.canDoRole(Role.READER, source)) {
            return new ResolvedItemLinkPath(path: absolutePath)
        }
        new ResolvedItemLinkPath(path: absolutePath, source: source)
    }

    private Model versionOwningModel(AdministeredItem item) {
        pathRepository.readParentItems(item)
        item.updatePath()
        Model model = item.owner
        if (!model) return null
        pathRepository.readParentItems(model)
        model.updatePath()
        model
    }
}
