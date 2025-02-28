package uk.ac.ox.softeng.mauro.controller.tree

import io.micronaut.http.HttpStatus
import io.micronaut.http.exceptions.HttpStatusException
import io.micronaut.security.authentication.AuthorizationException
import uk.ac.ox.softeng.mauro.api.Paths
import uk.ac.ox.softeng.mauro.api.tree.TreeApi
import uk.ac.ox.softeng.mauro.audit.Audit

import groovy.transform.CompileStatic
import io.micronaut.core.annotation.Nullable
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.QueryValue
import io.micronaut.security.annotation.Secured
import io.micronaut.security.rules.SecurityRule
import jakarta.inject.Inject
import uk.ac.ox.softeng.mauro.domain.datamodel.DataClass
import uk.ac.ox.softeng.mauro.domain.datamodel.DataElement
import uk.ac.ox.softeng.mauro.domain.datamodel.DataModel
import uk.ac.ox.softeng.mauro.domain.folder.Folder
import uk.ac.ox.softeng.mauro.domain.model.AdministeredItem
import uk.ac.ox.softeng.mauro.domain.model.Item
import uk.ac.ox.softeng.mauro.domain.model.Model
import uk.ac.ox.softeng.mauro.domain.model.Path
import uk.ac.ox.softeng.mauro.domain.security.Role
import uk.ac.ox.softeng.mauro.domain.tree.TreeItem
import uk.ac.ox.softeng.mauro.persistence.cache.AdministeredItemCacheableRepository
import uk.ac.ox.softeng.mauro.persistence.cache.ModelCacheableRepository.FolderCacheableRepository
import uk.ac.ox.softeng.mauro.persistence.classifier.ClassifierContentRepository
import uk.ac.ox.softeng.mauro.persistence.model.PathRepository
import uk.ac.ox.softeng.mauro.persistence.search.dto.SearchRepository
import uk.ac.ox.softeng.mauro.persistence.search.dto.SearchRequestDTO
import uk.ac.ox.softeng.mauro.persistence.search.dto.SearchResultsDTO
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

    @Inject
    PathRepository pathRepository

    @Inject
    SearchRepository searchRepository

    @Audit
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

    @Audit
    @Get(Paths.TREE_ITEM)
    List<TreeItem> itemTree(String domainType, UUID id, @Nullable @QueryValue Boolean foldersOnly) {
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

    @Get(Paths.TREE_ITEM_ANCESTORS)
    TreeItem itemTreeAncestors(String domainType, UUID id)
    {
        AdministeredItemCacheableRepository repository = repositoryService.getAdministeredItemRepository(domainType)

        Item item = repository.readById(id)
        if(!item instanceof AdministeredItem)
        {
            throw new HttpStatusException(HttpStatus.BAD_REQUEST, "$domainType cannot be used here")
        }

        AdministeredItem aditem = (AdministeredItem) item

        accessControlService.checkRole(Role.READER, aditem)

        pathRepository.readParentItems(aditem)

        Path path = aditem.getPathToEdge()


        AdministeredItem parentFolder=(AdministeredItem) path.findAncestorNodeItem(id, 'Folder')
        if(parentFolder==null)
        {
            parentFolder=(AdministeredItem) path.findAncestorNodeItem(id, 'VersionedFolder')
        }

        if(parentFolder==null)
        {
            throw new HttpStatusException(HttpStatus.NOT_FOUND, "Item $id does not have a parent folder")
        }

        accessControlService.checkRole(Role.READER, parentFolder)

        List<TreeItem> treeItems = filterTreeByReadable(treeService.buildTree(parentFolder, false, true))

        if(treeItems.isEmpty())
        {
            throw new HttpStatusException(HttpStatus.NOT_FOUND, "Item $id does not have a parent folder")
        }

        return treeItems.get(treeItems.size()-1)
    }

    @Get(Paths.TREE_FOLDER_ANCESTORS)
    TreeItem folderTreeAncestors(UUID id)
    {
        AdministeredItemCacheableRepository repository = repositoryService.getAdministeredItemRepository("folder")

        AdministeredItem aditem = (AdministeredItem) repository.readById(id)

        accessControlService.checkRole(Role.READER, aditem)

        pathRepository.readParentItems(aditem)

        Path path = aditem.getPathToEdge()

        AdministeredItem parentFolder=(AdministeredItem) path.findAncestorNodeItem(id, 'Folder')
        if(parentFolder==null)
        {
            parentFolder=(AdministeredItem) path.findAncestorNodeItem(id, 'VersionedFolder')
        }

        if(parentFolder==null)
        {
            parentFolder=aditem
        }
        else {
            accessControlService.checkRole(Role.READER, parentFolder)
        }

        List<TreeItem> treeItems = filterTreeByReadable(treeService.buildTree(parentFolder, false, true))

        if(treeItems.isEmpty())
        {
            throw new HttpStatusException(HttpStatus.NOT_FOUND, "Item $id does not have a parent folder")
        }

        return treeItems.get(treeItems.size()-1)
    }

    @Audit
    @Get(Paths.TREE_FOLDER_SEARCH)
    List<SearchResultsDTO> itemTreeSearch(String searchTerm) {

        SearchRequestDTO requestDTO=new SearchRequestDTO()
        requestDTO.searchTerm=searchTerm

        List<SearchResultsDTO> results=searchRepository.search(requestDTO)

        Set<UUID> ids=[]

        List<SearchResultsDTO> resultItems = []
        throughResults:
        for(int r=0;r<results.size();r++)
        {
            SearchResultsDTO searchResultsDTO=results.get(r)
            String domainType=searchResultsDTO.domainType
            AdministeredItemCacheableRepository repository = repositoryService.getAdministeredItemRepository(domainType)
            if(repository==null){continue}
            UUID id=searchResultsDTO.id
            Item item = repository.readById(id)
            if(!item instanceof AdministeredItem){continue}
            AdministeredItem adItem=(AdministeredItem) item
            try {
                accessControlService.checkRole(Role.READER, adItem)
            }
            catch(AuthorizationException ae)
            {
                continue throughResults
            }

            pathRepository.readParentItems(adItem)

            if(adItem.owner == null){continue}

            UUID modelId=adItem.owner.id
            if(! (adItem instanceof DataClass || adItem instanceof DataModel || adItem instanceof Folder))
            {
                Float tsRank=searchResultsDTO.tsRank

                while(! (adItem instanceof DataClass || adItem instanceof DataModel || adItem instanceof Folder))
                {
                    adItem=adItem.getParent()
                    if(adItem==null)
                    {
                        continue throughResults
                    }
                }

                searchResultsDTO=new SearchResultsDTO()
                searchResultsDTO.id=adItem.id
                searchResultsDTO.domainType=adItem.domainType
                searchResultsDTO.label=adItem.label
                searchResultsDTO.description=adItem.description
                searchResultsDTO.dateCreated=adItem.dateCreated
                searchResultsDTO.lastUpdated=adItem.lastUpdated
                searchResultsDTO.tsRank=tsRank
            }

            searchResultsDTO.modelId=modelId

            if(!ids.contains(searchResultsDTO.id)) {
                resultItems.addAll(searchResultsDTO)
                ids.add(searchResultsDTO.id)
            }
        }

        return resultItems
    }

}
