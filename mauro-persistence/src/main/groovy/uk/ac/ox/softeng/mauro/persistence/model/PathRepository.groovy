package uk.ac.ox.softeng.mauro.persistence.model

import uk.ac.ox.softeng.mauro.domain.model.AdministeredItem
import uk.ac.ox.softeng.mauro.domain.model.Path
import uk.ac.ox.softeng.mauro.exception.MauroInternalException
import uk.ac.ox.softeng.mauro.persistence.cache.AdministeredItemCacheableRepository

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
        cacheableRepositories.find {it.handles(item.class)} ?: administeredItemRepositories.find {it.handles(item.class)}
    }
}
