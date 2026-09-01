package org.maurodata.controller.tree

import groovy.util.logging.Slf4j
import io.swagger.v3.oas.annotations.Operation
import org.maurodata.api.Paths
import org.maurodata.api.tree.TreeApi
import org.maurodata.audit.Audit
import org.maurodata.controller.model.AvailableActions
import org.maurodata.domain.datamodel.DataClass
import org.maurodata.domain.datamodel.DataModel
import org.maurodata.domain.folder.Folder
import org.maurodata.domain.model.AdministeredItem
import org.maurodata.domain.model.Item
import org.maurodata.domain.model.Model
import org.maurodata.domain.model.Path
import org.maurodata.domain.security.Role
import org.maurodata.domain.security.SecurableResourceGroupRole
import org.maurodata.domain.security.UserGroup
import org.maurodata.domain.tree.TreeItem
import org.maurodata.persistence.ContentHandler
import org.maurodata.persistence.ContentsService
import org.maurodata.persistence.cache.AdministeredItemCacheableRepository
import org.maurodata.persistence.cache.ItemCacheableRepository
import org.maurodata.persistence.cache.ItemCacheableRepository.UserGroupCacheableRepository
import org.maurodata.persistence.cache.ModelCacheableRepository.FolderCacheableRepository
import org.maurodata.persistence.classifier.ClassificationSchemeRepository
import org.maurodata.persistence.datamodel.DataModelRepository
import org.maurodata.persistence.dto.HasChildrenDTO
import org.maurodata.persistence.model.PathRepository
import org.maurodata.persistence.search.SearchRepository
import org.maurodata.domain.search.dto.SearchRequestDTO
import org.maurodata.domain.search.dto.SearchResultsDTO
import org.maurodata.persistence.service.RepositoryService
import org.maurodata.persistence.service.TreeService
import org.maurodata.persistence.terminology.TerminologyRepository
import org.maurodata.security.AccessControlService

import groovy.transform.CompileStatic
import io.micronaut.core.annotation.Nullable
import io.micronaut.http.HttpStatus
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.QueryValue
import io.micronaut.http.exceptions.HttpStatusException
import io.micronaut.security.annotation.Secured
import io.micronaut.security.authentication.AuthorizationException
import io.micronaut.security.rules.SecurityRule
import jakarta.inject.Inject

@CompileStatic
@Controller
@Slf4j
@Secured(SecurityRule.IS_ANONYMOUS)
class TreeController implements TreeApi {

    @Inject
    TreeService treeService

    @Inject
    RepositoryService repositoryService

    @Inject
    FolderCacheableRepository folderRepository

    @Inject
    DataModelRepository dataModelRepository

    @Inject
    TerminologyRepository terminologyRepository

    @Inject
    ClassificationSchemeRepository classificationSchemeRepository

    @Inject
    UserGroupCacheableRepository userGroupRepository

    @Inject
    ItemCacheableRepository.SecurableResourceGroupRoleCacheableRepository securableResourceGroupRoleRepository

    @Inject
    AccessControlService accessControlService

    @Inject
    PathRepository pathRepository

    @Inject
    SearchRepository searchRepository

    @Inject
    ContentsService contentsService

    TreeController() {
    }


    @Audit
    @Operation(summary = "List the trees", description = "Returns the trees. You must have read privileges on the item in question.")
    @Get(Paths.TREE_FOLDER)
    List<TreeItem> folderTree(@Nullable UUID id, @Nullable @QueryValue Boolean foldersOnly) {

        long startTime = System.currentTimeMillis()
        Folder rootFolder = null
        if (id) {
            rootFolder = folderRepository.readById(id)
            accessControlService.checkRole(Role.READER, rootFolder)
        }
        log.trace("Time taken 1: {}", System.currentTimeMillis() - startTime)
        startTime = System.currentTimeMillis()

        ContentHandler contentHandler = contentsService.loadTree(rootFolder, foldersOnly?:false) // rootFolder may be null
        log.trace("Time taken 2: {}", System.currentTimeMillis() - startTime)
        startTime = System.currentTimeMillis()
        List<SecurableResourceGroupRole> userRoles = []
        if(accessControlService.userAuthenticated) {
            List<UserGroup> userGroups = userGroupRepository.readAllByCatalogueUserId(accessControlService.userId)
            userRoles = securableResourceGroupRoleRepository.readAllByUserGroupIdIn(userGroups.id)
        }
        log.trace("Time taken 3: {}", System.currentTimeMillis() - startTime)
        startTime = System.currentTimeMillis()

        // Now we filter the tree by what we can read
        Set<UUID> readableItems = [] as HashSet<UUID>
        if(accessControlService.isAdministrator()) {
            readableItems = contentHandler.allItems.keySet()
        } else {
            boolean userAuthenticated = accessControlService.userAuthenticated
            Set<UUID> roleAllowedIds = userRoles.collect {it.securableResourceId } as Set
            log.trace("Time taken 4: {}", System.currentTimeMillis() - startTime)
            startTime = System.currentTimeMillis()

            contentHandler.allItems.values().each {AdministeredItem administeredItem ->
                // We know these are really models
                Model model = (Model) administeredItem
                if (readableItems.contains(model.id)) {
                    // We've already seen this folder somehow
                    return
                }
                if (model.readableByEveryone
                    ||  (model.readableByAuthenticatedUsers && userAuthenticated)
                    ||  roleAllowedIds.contains(model.id)
                    || model.id == id // We've already checked this model is readable, and it might be readable via its parent
                ) {
                    // First make parents visible
                    Model m = model
                    while(m) {
                        readableItems.add(m.id)
                        m = m.parent
                    }
                    // Then make all children visible
                    if(model instanceof Folder) {
                        makeChildrenVisible(model, readableItems)
                    }

                }
            }
            log.trace("Time taken 5: {}", System.currentTimeMillis() - startTime)
            startTime = System.currentTimeMillis()

        }
        Set<Model> allModels = [] as Set<Model>

        Set<UUID> hasChildren = [] as Set<UUID>

        if(id) {
            Folder originalFolder = contentHandler.folders[0].first()
            allModels.addAll(originalFolder.childFolders.findAll {readableItems.contains(it.id)})
            allModels.addAll(originalFolder.classificationSchemes.findAll {readableItems.contains(it.id)})
            allModels.addAll(originalFolder.terminologies.findAll {readableItems.contains(it.id)})
            allModels.addAll(originalFolder.codeSets.findAll {readableItems.contains(it.id)})
            allModels.addAll(originalFolder.dataModels.findAll {readableItems.contains(it.id)})

            dataModelRepository.getHasChildrenDTOs(originalFolder.dataModels.findAll {readableItems.contains(it.id)}.id).each {
                if(it.hasChildren) {
                    hasChildren.add(it.id)
                }
            }
            terminologyRepository.getHasChildrenDTOs(originalFolder.terminologies.findAll {readableItems.contains(it.id)}.id).each {
                if(it.hasChildren) {
                    hasChildren.add(it.id)
                }
            }
            classificationSchemeRepository.getHasChildrenDTOs(originalFolder.classificationSchemes.findAll {readableItems.contains(it.id)}.id).each {
                if(it.hasChildren) {
                    hasChildren.add(it.id)
                }
            }
        } else {
            allModels.addAll(contentHandler.folders[0].findAll{readableItems.contains(it.id)})
        }

        List<TreeItem> items = allModels.collect {TreeItem ti = TreeItem.from(it)
            if(it instanceof Folder) {
                ti.hasChildren = (it.childFolders || it.classificationSchemes || it.terminologies || it.codeSets || it.dataModels)
            } else {
                ti.hasChildren = hasChildren.contains(it.id)
            }
            return ti
        }
        return items.sort{it.label + it.branchName + it.modelVersionTag + it.modelVersion}
    }




    void makeChildrenVisible(Folder folder, Set<UUID> readableItems) {
        readableItems.addAll(folder.classificationSchemes.id as Set<UUID>)
        readableItems.addAll(folder.terminologies.id as Set<UUID>)
        readableItems.addAll(folder.codeSets.id as Set<UUID>)
        readableItems.addAll(folder.dataModels.id as Set<UUID>)
        folder.childFolders.each {childFolder ->
            readableItems.add(childFolder.id)
            makeChildrenVisible(childFolder, readableItems)
        }
    }

    @Audit
    @Operation(summary = "List the trees", description = "Returns the trees. You must have read privileges on the item in question.")
    @Get(Paths.TREE_ITEM)
    List<TreeItem> itemTree(String domainType, UUID id, @Nullable @QueryValue Boolean foldersOnly) {
        foldersOnly = foldersOnly ?: false
        AdministeredItemCacheableRepository repository = repositoryService.getAdministeredItemRepository(domainType)
        AdministeredItem item = (AdministeredItem) repository.readById(id)
        pathRepository.readParentItems(item)
        item.updatePath()
        AvailableActions.updateAvailableActions(item, accessControlService)

        accessControlService.checkRole(Role.READER, item)
        List<TreeItem> treeItems = filterTreeByReadable(treeService.buildTree(item, domainType.contains(Folder.simpleName) ? foldersOnly : false, false, true))
        treeItems
    }

    protected List<TreeItem> filterTreeByReadable(List<TreeItem> treeItems) {
        treeItems.each {
            if (!it.item) {
                throw new IllegalArgumentException('TreeItem must have item set for security check')
            }
        }
        treeItems = treeItems.findAll {accessControlService.canDoRole(Role.READER, it.item)}
        treeItems.each {
            it.children = it.children.findAll {accessControlService.canDoRole(Role.READER, it.item)}
            AvailableActions.updateAvailableActions(it.item, accessControlService)
            if (it.item instanceof Model) {
                it.modelVersion = ((Model) it.item).modelVersion
                it.modelVersionTag = ((Model) it.item).modelVersionTag
            }
            it.availableActions = new ArrayList<String>(it.item.availableActions?:[])
            it.children.each {
                AvailableActions.updateAvailableActions(it.item, accessControlService)
                it.availableActions = new ArrayList<String>(it.item.availableActions?:[])
                if (it.item instanceof Model) {
                    it.modelVersion = ((Model) it.item).modelVersion
                    it.modelVersionTag = ((Model) it.item).modelVersionTag
                }
            }
        }
        treeItems
    }

    /*
    This is actually a path of ancestors leading to this item.
    This is not just folders
    There must be no other siblings, the UI doesn't check where the item
    is in the returned structure to determine the path
     */

    @Operation(summary = "Get a tree", description = "Returns a tree. You must have read privileges on the item in question.")
    @Get(Paths.TREE_ITEM_ANCESTORS)
    TreeItem itemTreeAncestors(String domainType, UUID id) {
        AdministeredItemCacheableRepository repository = repositoryService.getAdministeredItemRepository(domainType)

        Item item = repository.readById(id)
        if (!item instanceof AdministeredItem) {
            throw new HttpStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "$domainType cannot be used here")
        }

        AdministeredItem aditem = (AdministeredItem) item

        accessControlService.checkRole(Role.READER, aditem)

        pathRepository.readParentItems(aditem)

        Path path = aditem.getPathToEdge()
        TreeItem currentParent = treeService.buildTreeItemForThis(aditem, false, false, false)
        UUID currentId = id

        for (; ;) {
            AdministeredItem parentAdministeredItem = (AdministeredItem) path.findAncestorNodeItem(currentId, null)

            if (parentAdministeredItem == null) {
                break
            }

            try {
                accessControlService.checkRole(Role.READER, parentAdministeredItem)
            }
            catch (AuthorizationException ae) {
                break
            }

            final TreeItem parentTreeItem = treeService.buildTreeItemForThis(parentAdministeredItem, false, false, false)
            if (currentParent == null) {
                currentParent = parentTreeItem
            } else {
                parentTreeItem.children = [currentParent]
                parentTreeItem.hasChildren = true
                currentParent = parentTreeItem
            }
            currentId = currentParent.id
        }

        return currentParent
    }

    @Operation(summary = "Get a tree", description = "Returns a tree. You must have read privileges on the item in question.")
    @Get(Paths.TREE_FOLDER_ANCESTORS)
    TreeItem folderTreeAncestors(UUID id) {
        AdministeredItemCacheableRepository repository = repositoryService.getAdministeredItemRepository("folder")

        AdministeredItem aditem = (AdministeredItem) repository.readById(id)

        accessControlService.checkRole(Role.READER, aditem)

        pathRepository.readParentItems(aditem)

        Path path = aditem.getPathToEdge()
        TreeItem currentParent = null
        UUID currentId = id

        for (; ;) {
            AdministeredItem parentAdministeredItem = (AdministeredItem) path.findAncestorNodeItem(currentId, null)

            if (parentAdministeredItem == null) {
                break
            }

            try {
                accessControlService.checkRole(Role.READER, parentAdministeredItem)
            }
            catch (AuthorizationException ae) {
                break
            }

            final TreeItem parentTreeItem = treeService.buildTreeItemForThis(parentAdministeredItem, false, false, false)
            if (currentParent == null) {
                currentParent = parentTreeItem
            } else {
                parentTreeItem.children = [currentParent]
                parentTreeItem.hasChildren = true
                currentParent = parentTreeItem
            }
            currentId = currentParent.id
        }

        if (currentParent == null) {
            currentParent = treeService.buildTreeItemForThis(aditem, true, false, true)
        }

        return currentParent
    }

    @Audit
    @Operation(summary = "List the trees", description = "Returns the trees. You must have read privileges on the item in question.")
    @Get(Paths.TREE_FOLDER_SEARCH)
    List<SearchResultsDTO> itemTreeSearch(String searchTerm) {

        SearchRequestDTO requestDTO = new SearchRequestDTO()
        requestDTO.searchTerm = searchTerm

        List<SearchResultsDTO> results = searchRepository.search(requestDTO)

        Set<UUID> ids = []

        List<SearchResultsDTO> resultItems = []
        throughResults:
        for (int r = 0; r < results.size(); r++) {
            SearchResultsDTO searchResultsDTO = results.get(r)
            String domainType = searchResultsDTO.domainType
            AdministeredItemCacheableRepository repository = repositoryService.getAdministeredItemRepository(domainType)
            if (repository == null) {
                continue
            }
            UUID id = searchResultsDTO.id
            Item item = repository.readById(id)
            if (!item instanceof AdministeredItem) {
                continue
            }
            AdministeredItem adItem = (AdministeredItem) item
            try {
                accessControlService.checkRole(Role.READER, adItem)
            }
            catch (AuthorizationException ae) {
                continue throughResults
            }

            pathRepository.readParentItems(adItem)

            if (adItem.owner == null) {
                continue
            }

            UUID modelId = adItem.owner.id
            if (!(adItem instanceof DataClass || adItem instanceof DataModel || adItem instanceof Folder)) {
                Float tsRank = searchResultsDTO.tsRank

                while (!(adItem instanceof DataClass || adItem instanceof DataModel || adItem instanceof Folder)) {
                    adItem = adItem.getParent()
                    if (adItem == null) {
                        continue throughResults
                    }
                }

                searchResultsDTO = new SearchResultsDTO()
                searchResultsDTO.id = adItem.id
                searchResultsDTO.domainType = adItem.domainType
                searchResultsDTO.label = adItem.label
                searchResultsDTO.description = adItem.description
                searchResultsDTO.dateCreated = adItem.dateCreated
                searchResultsDTO.lastUpdated = adItem.lastUpdated
                searchResultsDTO.tsRank = tsRank
            }

            searchResultsDTO.modelId = modelId

            if (!ids.contains(searchResultsDTO.id)) {
                resultItems.addAll(searchResultsDTO)
                ids.add(searchResultsDTO.id)
            }
        }

        return resultItems
    }

}
