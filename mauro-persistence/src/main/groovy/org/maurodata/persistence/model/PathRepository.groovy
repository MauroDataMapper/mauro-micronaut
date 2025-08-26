package org.maurodata.persistence.model

import org.maurodata.domain.model.AdministeredItem
import org.maurodata.domain.model.ItemReference
import org.maurodata.domain.model.Path
import org.maurodata.exception.MauroInternalException
import org.maurodata.persistence.cache.AdministeredItemCacheableRepository

import groovy.transform.CompileStatic
import io.micronaut.core.annotation.NonNull
import jakarta.inject.Inject
import jakarta.inject.Singleton

@CompileStatic
@Singleton
class PathRepository {

    @Inject
    List<AdministeredItemRepository> administeredItemRepositories

    @Inject
    List<AdministeredItemCacheableRepository> cacheableRepositories

    List<AdministeredItem> readParentItems(AdministeredItem item) {
        List<AdministeredItem> items = []
        int i = 0
        AdministeredItem node = item.id ? (AdministeredItem) getRepository(item).readById(item.id) : item
        while (node) {
            items.add(node)
            if (node.parent) node = (AdministeredItem) getRepository(node.parent).readById(node.parent.id)
            else node = null
            i++
            if (i > Path.PATH_MAX_NODES) throw new MauroInternalException("Path exceeded maximum depth of [$Path.PATH_MAX_NODES]")
        }
        if (items[1]) item.parent = items[1]
        items.eachWithIndex {AdministeredItem it, Integer j ->
            if (items[j+1]) it.parent = items[j+1]
        }
    }

    @NonNull
    AdministeredItemRepository getRepository(AdministeredItem item) {
        cacheableRepositories.find {it.handles(item.class) || it.handles(item.domainType)} ?: administeredItemRepositories.find {it.handles(item.class) || it.handles(item.domainType)}
    }

    @NonNull
    AdministeredItemRepository getRepositoryForPathPrefix(final String prefix) {
        cacheableRepositories.find {it.handlesPathPrefix(prefix)} ?: administeredItemRepositories.find {it.handlesPathPrefix(prefix)}
    }

    @NonNull
    AdministeredItemRepository getRepositoryForDomainType(final String domainType) {
        cacheableRepositories.find {it.handles(domainType)} ?: administeredItemRepositories.find {it.handles(domainType)}
    }

    AdministeredItem findResourcesByPathFromRootResource(final AdministeredItem resource, final Path path) {
        findResourcesByPathFromRootResource(resource, path, 1)
    }

    protected AdministeredItem findResourcesByPathFromRootResource(final AdministeredItem resource, final Path path, final int positionInPath) {

        if (positionInPath >= path.nodes.size()) {
            readParentItems(resource)
            resource.updatePath()
            return resource
        }
        final Path.PathNode currentNode = path.nodes.get(positionInPath)
        final AdministeredItemRepository prefixRepository = getRepositoryForPathPrefix(currentNode.prefix)
        if (prefixRepository == null) {throw new MauroInternalException("No repository claims to handle the prefix " + currentNode.prefix)}
        final List<AdministeredItem> children = prefixRepository.findAllByParentAndPathIdentifier(resource.id, currentNode.identifier)
        if (children.size() > 1) {throw new MauroInternalException("More than one Item returned for " + currentNode.prefix + " " + currentNode.identifier)}
        if (children.size() == 0) {
            return null
        }
        final AdministeredItem currentAdministeredItem = (AdministeredItem) children.get(0)
        return findResourcesByPathFromRootResource(currentAdministeredItem, path, positionInPath + 1)
    }

    List<Path> resolveItemReferences(final List<ItemReference> itemReferences) {
        final List<Path> resolved = []
        itemReferences.forEach {ItemReference itemReference ->

            if (itemReference.pathToItem != null) {
                resolved << itemReference.pathToItem
            } else {
                final AdministeredItemRepository administeredItemRepository = getRepositoryForDomainType(itemReference.itemDomainType)
                final AdministeredItem resolvedAdministeredItem = (AdministeredItem) administeredItemRepository.findById(itemReference.itemId)
                if (resolvedAdministeredItem == null) {throw new MauroInternalException("Did not find reference to " + itemReference.toString())}
                readParentItems(resolvedAdministeredItem)
                resolvedAdministeredItem.updatePath()
                resolved << resolvedAdministeredItem.getPathToEdge()
            }
        }
        return resolved
    }
}
