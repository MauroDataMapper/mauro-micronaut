package uk.ac.ox.softeng.mauro.controller.federation

import uk.ac.ox.softeng.mauro.ErrorHandler
import uk.ac.ox.softeng.mauro.controller.Paths
import uk.ac.ox.softeng.mauro.controller.model.ItemController
import uk.ac.ox.softeng.mauro.domain.federation.SubscribedCatalogue
import uk.ac.ox.softeng.mauro.domain.federation.SubscribedModel
import uk.ac.ox.softeng.mauro.domain.federation.SubscribedModelFederationParams
import uk.ac.ox.softeng.mauro.domain.folder.Folder
import uk.ac.ox.softeng.mauro.domain.model.Model
import uk.ac.ox.softeng.mauro.persistence.cache.ItemCacheableRepository
import uk.ac.ox.softeng.mauro.persistence.cache.ItemCacheableRepository.SubscribedCatalogueCacheableRepository
import uk.ac.ox.softeng.mauro.persistence.cache.ModelCacheableRepository.FolderCacheableRepository
import uk.ac.ox.softeng.mauro.service.federation.SubscribedModelService
import uk.ac.ox.softeng.mauro.web.ListResponse

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import io.micronaut.core.annotation.NonNull
import io.micronaut.http.HttpStatus
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Post
import io.micronaut.scheduling.TaskExecutors
import io.micronaut.scheduling.annotation.ExecuteOn
import io.micronaut.security.annotation.Secured
import io.micronaut.security.rules.SecurityRule
import io.micronaut.transaction.annotation.Transactional
import jakarta.inject.Inject

@Slf4j
@Controller()
@CompileStatic
@Secured(SecurityRule.IS_ANONYMOUS)
class SubscribedModelController extends ItemController<SubscribedModel> {

    final FolderCacheableRepository folderCacheableRepository
    final SubscribedModelService subscribedModelService
    final ItemCacheableRepository.SubscribedModelCacheableRepository subscribedModelCacheableRepository
    final ItemCacheableRepository.SubscribedCatalogueCacheableRepository subscribedCatalogueCacheableRepository

    @Inject
    SubscribedModelController(ItemCacheableRepository.SubscribedModelCacheableRepository subscribedModelCacheableRepository,
                              SubscribedCatalogueCacheableRepository subscribedCatalogueCacheableRepository,
                              FolderCacheableRepository folderCacheableRepository,
                              SubscribedModelService subscribedModelService) {
        super(subscribedModelCacheableRepository)
        this.subscribedModelCacheableRepository = subscribedModelCacheableRepository
        this.subscribedCatalogueCacheableRepository = subscribedCatalogueCacheableRepository
        this.folderCacheableRepository = folderCacheableRepository
        this.subscribedModelService = subscribedModelService
    }

    @Get(Paths.SUBSCRIBED_MODELS_ROUTE)
    ListResponse<SubscribedModel> listAll(@NonNull UUID subscribedCatalogueId) {
        accessControlService.checkAdministrator()
        ListResponse.from(subscribedModelCacheableRepository.findAllBySubscribedCatalogueId(subscribedCatalogueId))
    }

    @Post(Paths.SUBSCRIBED_MODELS_ROUTE)
    @ExecuteOn(TaskExecutors.BLOCKING)
    @Transactional
    SubscribedModel create(@NonNull UUID subscribedCatalogueId, @Body @NonNull SubscribedModelFederationParams subscribedModelFederationParams) {
        accessControlService.checkAuthenticated()
        SubscribedCatalogue subscribedCatalogue = subscribedCatalogueCacheableRepository.readById(subscribedCatalogueId)
        ErrorHandler.handleError(HttpStatus.UNPROCESSABLE_ENTITY, subscribedCatalogue,"Subscribed Catalogue not found $subscribedCatalogueId")

        Folder folder = folderCacheableRepository.readById(subscribedModelFederationParams.subscribedModel?.folderId)

        ErrorHandler.handleError(HttpStatus.NOT_FOUND, folder,"Entity not found, $subscribedModelFederationParams.subscribedModel.folderId")
     //   accessControlService.checkRole(Role.READER, folder)

        SubscribedModel subscribedModel = subscribedModelFederationParams.subscribedModel
        subscribedModel.subscribedCatalogue = subscribedCatalogue
        Model importedModel = subscribedModelService.exportFederatedModelAndImport(subscribedModelFederationParams, folder, subscribedModel)

        //todo versionLinks
        subscribedModel.localModelId = importedModel.id
        subscribedModel.subscribedModelType = importedModel.domainType
        subscribedModelCacheableRepository.save(subscribedModel)
    }

}


