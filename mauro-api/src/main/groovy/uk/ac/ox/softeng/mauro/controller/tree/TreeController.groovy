package uk.ac.ox.softeng.mauro.controller.tree

import groovy.transform.CompileStatic
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import jakarta.inject.Inject
import uk.ac.ox.softeng.mauro.domain.folder.Folder
import uk.ac.ox.softeng.mauro.domain.tree.TreeItem
import uk.ac.ox.softeng.mauro.persistence.cache.ModelCacheableRepository.FolderCacheableRepository
import uk.ac.ox.softeng.mauro.persistence.service.TreeService

@CompileStatic
@Controller('/tree')
class TreeController {

    @Inject
    TreeService treeService

    @Inject
    FolderCacheableRepository folderRepository

    @Get('/folders/{id}')
    List<TreeItem> folderTree(UUID id) {
        Folder folder = folderRepository.readById(id)
        treeService.buildTree(folder)
    }
}
