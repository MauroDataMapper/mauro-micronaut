package org.maurodata.controller.federation

import org.maurodata.audit.Audit

import io.micronaut.http.HttpResponse
import org.maurodata.ErrorHandler
import org.maurodata.api.Paths
import org.maurodata.controller.model.ItemController
import org.maurodata.domain.facet.federation.SubscribedCatalogue
import org.maurodata.domain.facet.federation.SubscribedModel
import org.maurodata.domain.facet.federation.SubscribedModelFederationParams
import org.maurodata.domain.folder.Folder
import org.maurodata.domain.model.Model
import org.maurodata.persistence.cache.ItemCacheableRepository
import org.maurodata.persistence.cache.ItemCacheableRepository.SubscribedCatalogueCacheableRepository
import org.maurodata.persistence.cache.ModelCacheableRepository.FolderCacheableRepository
import org.maurodata.service.federation.SubscribedModelService
import org.maurodata.web.ListResponse

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import io.micronaut.core.annotation.NonNull
import io.micronaut.core.annotation.Nullable
import io.micronaut.http.HttpStatus
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Delete
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Post
import io.micronaut.http.exceptions.HttpStatusException
import io.micronaut.scheduling.TaskExecutors
import io.micronaut.scheduling.annotation.ExecuteOn
import io.micronaut.security.annotation.Secured
import io.micronaut.security.rules.SecurityRule
import io.micronaut.transaction.annotation.Transactional
import jakarta.inject.Inject

@Slf4j
@Controller
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

    @Audit
    @Get(Paths.SUBSCRIBED_MODELS_LIST)
    ListResponse<SubscribedModel> listAll(@NonNull UUID subscribedCatalogueId) {
        accessControlService.checkAuthenticated()
        ListResponse.from(subscribedModelCacheableRepository.findAllBySubscribedCatalogueId(subscribedCatalogueId))
    }

    @Audit
    @Get(Paths.SUBSCRIBED_MODELS_ID)
    SubscribedModel show(@NonNull UUID subscribedCatalogueId, @NonNull UUID subscribedModelId) {
        accessControlService.checkAuthenticated()

        SubscribedCatalogue subscribedCatalogue = subscribedCatalogueCacheableRepository.readById(subscribedCatalogueId)
        ErrorHandler.handleErrorOnNullObject(HttpStatus.UNPROCESSABLE_ENTITY, subscribedCatalogue, "Subscribed Catalogue not found $subscribedCatalogueId")

        subscribedModelCacheableRepository.findBySubscribedModelIdAndSubscribedCatalogueId(subscribedModelId, subscribedCatalogue)
    }

    @Audit(level = Audit.AuditLevel.FILE_ONLY)
    @Post(Paths.SUBSCRIBED_MODELS_LIST)
    @ExecuteOn(TaskExecutors.BLOCKING)
    @Transactional
    SubscribedModel create(@NonNull UUID subscribedCatalogueId, @Body @NonNull SubscribedModelFederationParams subscribedModelFederationParams) {
        accessControlService.checkAuthenticated()

        SubscribedCatalogue subscribedCatalogue = subscribedCatalogueCacheableRepository.readById(subscribedCatalogueId)
        ErrorHandler.handleErrorOnNullObject(HttpStatus.UNPROCESSABLE_ENTITY, subscribedCatalogue, "Subscribed Catalogue not found $subscribedCatalogueId")

        Folder folder = folderCacheableRepository.readById(subscribedModelFederationParams.subscribedModel?.folderId)

        ErrorHandler.handleErrorOnNullObject(HttpStatus.NOT_FOUND, folder, "Entity not found, $subscribedModelFederationParams.subscribedModel.folderId")
        //   accessControlService.checkRole(Role.READER, folder)

        SubscribedModel subscribedModel = subscribedModelFederationParams.subscribedModel
        subscribedModel.subscribedCatalogue = subscribedCatalogue
        Model importedModel = subscribedModelService.exportFederatedModelAndImport(subscribedModelFederationParams, folder, subscribedModel)

        //todo versionLinks
        subscribedModel.localModelId = importedModel.id
        subscribedModel.subscribedModelType = importedModel.domainType
        SubscribedModel saved = subscribedModelCacheableRepository.save(subscribedModel)
        saved
    }

    @Audit(level = Audit.AuditLevel.FILE_ONLY)
    @Delete(Paths.SUBSCRIBED_MODELS_ID)
    @Transactional
    HttpResponse delete(@NonNull UUID subscribedCatalogueId, @NonNull UUID subscribedModelId, @Body @Nullable SubscribedModel subscribedModel) {
        accessControlService.checkAdministrator()

        SubscribedCatalogue subscribedCatalogue = subscribedCatalogueCacheableRepository.findById(subscribedCatalogueId)
        ErrorHandler.handleErrorOnNullObject(HttpStatus.NOT_FOUND, subscribedCatalogue, "Item $subscribedCatalogueId not found")

        SubscribedModel subscribedModelToDelete = subscribedModelCacheableRepository.findById(subscribedModelId)
        ErrorHandler.handleErrorOnNullObject(HttpStatus.NOT_FOUND, subscribedModelToDelete, "Item $subscribedModelId not found")

        if (subscribedModel?.version) subscribedModel.version = subscribedModel.version
        Long deleted = subscribedModelCacheableRepository.delete(subscribedModelToDelete)
        if (deleted) {
            HttpResponse.status(HttpStatus.NO_CONTENT)
        } else {
            throw new HttpStatusException(HttpStatus.NOT_FOUND, 'Not found for deletion')
        }
    }
}


