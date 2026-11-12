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

    /** Per-request caches; never shared between graph requests or callers. */
    static class ResolutionContext {
        final Map<UUID, Model> targetModels = [:]
        final Set<UUID> parentItemsLoaded = [] as Set<UUID>
        final Map<String, PathModelCandidates> visibleModelCandidates = [:]
        final Map<String, ResolvedItemLinkPath> resolvedPaths = [:]
        final Map<String, AdministeredItem> pathPrefixItems = [:]
    }

    static ResolutionContext newResolutionContext() { new ResolutionContext() }

    ResolvedItemLinkPath resolve(AdministeredItem target, Path storedPath) {
        resolve(newResolutionContext(), target, storedPath)
    }

    ResolvedItemLinkPath resolve(ResolutionContext context, AdministeredItem target, Path storedPath) {
        if (!storedPath || !storedPath.nodes) {
            return new ResolvedItemLinkPath(path: storedPath)
        }

        Path path = new Path(storedPath.toString())
        Model targetModel = versionOwningModel(context, target)
        if (!targetModel) {
            return new ResolvedItemLinkPath(path: path)
        }

        Path absolutePath = path
        Model sourceModel
        if (path.isAbsolute()) {
            Path.PathNode modelRoot = path.modelRootNode
            if (!modelRoot) {
                return new ResolvedItemLinkPath(path: path)
            }
            String rootKey = modelRoot.toString()
            PathModelCandidates candidates = context.visibleModelCandidates[rootKey]
            if (candidates == null) {
                candidates = visiblePathModelResolver.findVisibleModelCandidatesForPathRoot(modelRoot)
                context.visibleModelCandidates[rootKey] = candidates
            }
            if (candidates.finalisedModels.size() != 1 || candidates.draftModels) {
                return new ResolvedItemLinkPath(path: path)
            }
            sourceModel = candidates.finalisedModels[0]
        } else {
            Path.PathNode modelRoot = targetModel.path?.modelRootNode
            if (!modelRoot) {
                return new ResolvedItemLinkPath(path: path)
            }
            absolutePath = new Path(([modelRoot] + path.nodes) as List<Path.PathNode>)
            sourceModel = targetModel
        }

        String absolutePathKey = absolutePath.toString()
        ResolvedItemLinkPath cached = context.resolvedPaths[absolutePathKey]
        if (cached != null) {
            return cached
        }

        if (sourceModel.id && !context.parentItemsLoaded.contains(sourceModel.id)) {
            pathRepository.readParentItems(sourceModel)
            context.parentItemsLoaded.add(sourceModel.id)
        }
        sourceModel.updatePath()
        AdministeredItem source = pathRepository.findResourcesByPathFromRootResource(sourceModel, absolutePath, context.pathPrefixItems)
        if (!source) {
            ResolvedItemLinkPath unresolved = new ResolvedItemLinkPath(path: absolutePath)
            context.resolvedPaths[absolutePathKey] = unresolved
            return unresolved
        }

        if (source.id) {
            context.parentItemsLoaded.add(source.id)
        }
        source.updatePath()
        if (!accessControlService.canDoRole(Role.READER, source)) {
            ResolvedItemLinkPath inaccessible = new ResolvedItemLinkPath(path: absolutePath)
            context.resolvedPaths[absolutePathKey] = inaccessible
            return inaccessible
        }
        ResolvedItemLinkPath resolved = new ResolvedItemLinkPath(path: absolutePath, source: source)
        context.resolvedPaths[absolutePathKey] = resolved
        resolved
    }

    static void rememberTargetModel(ResolutionContext context, AdministeredItem item, Model model) {
        if (item?.id) {
            context.targetModels[item.id] = model
        }
    }

    private Model versionOwningModel(ResolutionContext context, AdministeredItem item) {
        if (context.targetModels.containsKey(item.id)) {
            return context.targetModels[item.id]
        }
        if (!context.parentItemsLoaded.contains(item.id)) {
            pathRepository.readParentItems(item)
            context.parentItemsLoaded.add(item.id)
        }
        item.updatePath()
        Model model = item.owner
        if (!model) {
            context.targetModels[item.id] = null
            return null
        }
        if (!context.parentItemsLoaded.contains(model.id)) {
            pathRepository.readParentItems(model)
            context.parentItemsLoaded.add(model.id)
        }
        model.updatePath()
        context.targetModels[item.id] = model
        model
    }
}
