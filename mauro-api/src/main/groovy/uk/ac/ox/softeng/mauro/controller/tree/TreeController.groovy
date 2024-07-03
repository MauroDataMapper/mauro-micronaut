package uk.ac.ox.softeng.mauro.controller.tree

import groovy.transform.CompileStatic
import io.micronaut.core.annotation.Nullable
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
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
@Controller('/tree')
@Secured(SecurityRule.IS_ANONYMOUS)
class TreeController {

    @Inject
    TreeService treeService

    @Inject
    RepositoryService repositoryService

    @Inject
    FolderCacheableRepository folderRepository

    @Inject
    AccessControlService accessControlService

    @Get('/folders{/id}')
    List<TreeItem> folderTree(@Nullable UUID id) {
        if (id) {
            Folder folder = folderRepository.readById(id)
            accessControlService.checkRole(Role.READER, folder)
            filterTreeByReadable(treeService.buildTree(folder))
        } else {
            filterTreeByReadable(treeService.buildRootFolderTree())
        }
    }

    @Get('/folders/{domainType}/{id}')
    List<TreeItem> itemTree(String domainType, UUID id) {
        AdministeredItemCacheableRepository repository = repositoryService.getAdministeredItemRepository(domainType)
        AdministeredItem item = repository.readById(id)
        accessControlService.checkRole(Role.READER, item)
        filterTreeByReadable(treeService.buildTree(item))
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
