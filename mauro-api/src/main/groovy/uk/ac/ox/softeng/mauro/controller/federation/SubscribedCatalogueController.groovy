package uk.ac.ox.softeng.mauro.controller.federation

import uk.ac.ox.softeng.mauro.ErrorHandler
import uk.ac.ox.softeng.mauro.controller.Paths
import uk.ac.ox.softeng.mauro.controller.model.ItemController
import uk.ac.ox.softeng.mauro.domain.federation.PublishedModel
import uk.ac.ox.softeng.mauro.domain.federation.SubscribedCatalogue
import uk.ac.ox.softeng.mauro.domain.federation.SubscribedCatalogueAuthenticationType
import uk.ac.ox.softeng.mauro.domain.federation.SubscribedCatalogueType
import uk.ac.ox.softeng.mauro.persistence.cache.ItemCacheableRepository
import uk.ac.ox.softeng.mauro.service.federation.SubscribedCatalogueService
import uk.ac.ox.softeng.mauro.web.ListResponse

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import io.micronaut.context.annotation.Value
import io.micronaut.core.annotation.NonNull
import io.micronaut.core.annotation.Nullable
import io.micronaut.http.HttpStatus
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Post
import io.micronaut.http.annotation.Put
import io.micronaut.http.annotation.QueryValue
import io.micronaut.http.exceptions.HttpStatusException
import io.micronaut.http.server.exceptions.HttpServerException
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
class SubscribedCatalogueController extends ItemController<SubscribedCatalogue> {

    @Value('${micronaut.federation.subscribed-catalogues.max}')
    int maxDefault

    ItemCacheableRepository.SubscribedCatalogueCacheableRepository subscribedCatalogueCacheableRepository

    final SubscribedCatalogueService subscribedCatalogueService

    @Inject
    SubscribedCatalogueController(ItemCacheableRepository.SubscribedCatalogueCacheableRepository subscribedCatalogueCacheableRepository,
                                  SubscribedCatalogueService subscribedCatalogueService) {
        super(subscribedCatalogueCacheableRepository)
        this.subscribedCatalogueCacheableRepository = subscribedCatalogueCacheableRepository
        this.subscribedCatalogueService = subscribedCatalogueService
    }

    @Get(Paths.ADMIN_SUBSCRIBED_CATALOGUES_ROUTE)
    ListResponse<SubscribedCatalogue> listAll() {
        accessControlService.checkAuthenticated()
        ListResponse.from(subscribedCatalogueCacheableRepository.findAll())
    }

    @Get(Paths.SUBSCRIBED_CATALOGUES_ID_ROUTE)
    SubscribedCatalogue show(@NonNull UUID subscribedCatalogueId) {
        accessControlService.checkAuthenticated()
        subscribedCatalogueCacheableRepository.findById(subscribedCatalogueId)
    }

    @Get(Paths.SUBSCRIBED_CATALOGUES_ROUTE)
    ListResponse<SubscribedCatalogue> listSubscribedCatalogues(@Nullable @QueryValue Integer max) {
        accessControlService.checkAuthenticated()
        max = max ?: maxDefault
        List<SubscribedCatalogue> subscribedCatalogues = subscribedCatalogueCacheableRepository.findAll()
        if (subscribedCatalogues.size() < max) {
            ListResponse.from(subscribedCatalogues)
        } else {
            List altered = subscribedCatalogues.subList(0, max)
            ListResponse.from(subscribedCatalogues.subList(0, max))
        }
    }

    @Post(Paths.ADMIN_SUBSCRIBED_CATALOGUES_ROUTE)
    @Transactional
    SubscribedCatalogue create(@Body @NonNull SubscribedCatalogue subscribedCatalogue) {
        accessControlService.checkAdministrator()
        cleanBody(subscribedCatalogue)
        updateCreationProperties(subscribedCatalogue)
        subscribedCatalogueCacheableRepository.save(subscribedCatalogue)
    }

    @Put(Paths.ADMIN_SUBSCRIBED_CATALOGUES_ID_ROUTE)
    @Transactional
    SubscribedCatalogue update(@NonNull UUID subscribedCatalogueId, @Body @NonNull SubscribedCatalogue subscribedCatalogue) {
        accessControlService.checkAdministrator()
        cleanBody(subscribedCatalogue)

        SubscribedCatalogue existing = subscribedCatalogueCacheableRepository.readById(subscribedCatalogueId)
        boolean hasChanged = updateProperties(existing, subscribedCatalogue)
        if (hasChanged) {
            subscribedCatalogueCacheableRepository.update(existing)
        } else {
            existing
        }
    }

    @Get(Paths.SUBSCRIBED_CATALOGUES_TYPES_ROUTE)
    ListResponse<SubscribedCatalogue> types() {
        accessControlService.checkAuthenticated()
        ListResponse.from(SubscribedCatalogueType.labels())
    }

    @Get(Paths.SUBSCRIBED_CATALOGUES_AUTHENTICATION_TYPES_ROUTE)
    ListResponse<SubscribedCatalogue> authenticationTypes() {
        accessControlService.checkAdministrator()
        ListResponse.from(SubscribedCatalogueAuthenticationType.labels())
    }

    @Get(Paths.SUBSCRIBED_CATALOGUES_TEST_CONNECTION_ROUTE)
    @ExecuteOn(TaskExecutors.BLOCKING)
    HttpStatus testConnection(@NonNull UUID subscribedCatalogueId) {
        accessControlService.checkAdministrator()

        SubscribedCatalogue subscribedCatalogue = subscribedCatalogueCacheableRepository.findById(subscribedCatalogueId)
        ErrorHandler.handleError(HttpStatus.NOT_FOUND, subscribedCatalogue, "Item $subscribedCatalogueId not found")
        try {
            List<PublishedModel> publishedModels = getFederatedPublishedModels(subscribedCatalogue)
        } catch (Exception e) {
            log.error(e.message)
            throw new HttpServerException(e.message, e)
        }
        return HttpStatus.OK
    }


    @Get(Paths.SUBSCRIBED_CATALOGUES_PUBLISHED_MODELS_ROUTE)
    @ExecuteOn(TaskExecutors.BLOCKING)
    ListResponse<PublishedModel> publishedModels(@NonNull UUID subscribedCatalogueId) {
        accessControlService.checkAuthenticated()

        SubscribedCatalogue subscribedCatalogue = subscribedCatalogueCacheableRepository.findById(subscribedCatalogueId)
        ErrorHandler.handleError(HttpStatus.NOT_FOUND, subscribedCatalogue, "Item $subscribedCatalogueId not found")
        ListResponse.from(getFederatedPublishedModels(subscribedCatalogue))
    }

    protected List<PublishedModel> getFederatedPublishedModels(SubscribedCatalogue subscribedCatalogue) {
        List<PublishedModel> publishedModels = []
        try {
            publishedModels = subscribedCatalogueService.getPublishedModels(subscribedCatalogue)
        } catch (Exception e) {
            throw new HttpStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.message)
        }
        publishedModels
    }

}





