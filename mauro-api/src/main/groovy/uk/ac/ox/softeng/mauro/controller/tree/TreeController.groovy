package uk.ac.ox.softeng.mauro.controller.tree

import uk.ac.ox.softeng.mauro.api.Paths
import uk.ac.ox.softeng.mauro.api.tree.TreeApi

import groovy.transform.CompileStatic
import io.micronaut.core.annotation.Nullable
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.QueryValue
import io.micronaut.security.annotation.Secured
import io.micronaut.security.rules.SecurityRule
import jakarta.inject.Inject
import uk.ac.ox.softeng.mauro.domain.folder.Folder
import uk.ac.ox.softeng.mauro.domain.model.AdministeredItem
import uk.ac.ox.softeng.mauro.domain.security.Role
import uk.ac.ox.softeng.mauro.domain.tree.TreeItem
import uk.ac.ox.softeng.mauro.persistence.cache.AdministeredItemCacheableRepository
import uk.ac.ox.softeng.mauro.persistence.cache.ModelCacheableRepository.FolderCacheableRepository
import uk.ac.ox.softeng.mauro.persistence.service.RepositoryService
import uk.ac.ox.softeng.mauro.persistence.service.TreeService
import uk.ac.ox.softeng.mauro.security.AccessControlService

@CompileStatic
@Controller
@Secured(SecurityRule.IS_ANONYMOUS)
class TreeController implements TreeApi {

    @Inject
    TreeService treeService

    @Inject
    RepositoryService repositoryService

    @Inject
    FolderCacheableRepository folderRepository

    @Inject
    AccessControlService accessControlService

    @Get(Paths.TREE_FOLDER)
    List<TreeItem> folderTree(@Nullable UUID id, @Nullable @QueryValue Boolean foldersOnly) {
        List<TreeItem> treeItems = []
        foldersOnly = foldersOnly ?: false
        if (id) {
            Folder folder = folderRepository.readById(id)
            accessControlService.checkRole(Role.READER, folder)
            treeItems = filterTreeByReadable(treeService.buildTree(folder, foldersOnly, true))
        } else {
            treeItems = filterTreeByReadable(treeService.buildRootFolderTree(foldersOnly))
        }
        treeItems
    }

    @Get(Paths.TREE_ITEM)
    List<TreeItem> itemTree(String domainType, UUID id, UUID id, @Nullable @QueryValue Boolean foldersOnly) {
        foldersOnly = foldersOnly ?: false
        AdministeredItemCacheableRepository repository = repositoryService.getAdministeredItemRepository(domainType)
        AdministeredItem item = repository.readById(id)
        accessControlService.checkRole(Role.READER, item)
        List<TreeItem> treeItems = filterTreeByReadable(treeService.buildTree(item, domainType.contains(Folder.class.simpleName) ? foldersOnly : false, true))
        treeItems
    }

    protected List<TreeItem> filterTreeByReadable(List<TreeItem> treeItems) {
        treeItems.each {if (!it.item) throw new IllegalArgumentException('TreeItem must have item set for security check')}
        treeItems = treeItems.findAll {accessControlService.canDoRole(Role.READER, it.item)}
        treeItems.each {
            it.children = it.children.findAll {accessControlService.canDoRole(Role.READER, it.item)}
        }
        treeItems
    }
}
