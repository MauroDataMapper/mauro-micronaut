package uk.ac.ox.softeng.mauro.controller.federation

import uk.ac.ox.softeng.mauro.controller.Paths
import uk.ac.ox.softeng.mauro.controller.model.ItemController
import uk.ac.ox.softeng.mauro.domain.federation.SubscribedCatalogue
import uk.ac.ox.softeng.mauro.domain.federation.SubscribedModel
import uk.ac.ox.softeng.mauro.domain.model.Item
import uk.ac.ox.softeng.mauro.persistence.cache.ItemCacheableRepository
import uk.ac.ox.softeng.mauro.persistence.cache.ItemCacheableRepository.SubscribedCatalogueCacheableRepository
import uk.ac.ox.softeng.mauro.web.ListResponse

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import io.micronaut.core.annotation.NonNull
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Post
import io.micronaut.security.annotation.Secured
import io.micronaut.security.rules.SecurityRule
import io.micronaut.transaction.annotation.Transactional

@Slf4j
@Controller()
@CompileStatic
@Secured(SecurityRule.IS_ANONYMOUS)
class SubscribedModelController extends ItemController<SubscribedModel> {

    ItemCacheableRepository.SubscribedModelCacheableRepository subscribedModelCacheableRepository
    ItemCacheableRepository.SubscribedCatalogueCacheableRepository subscribedCatalogueCacheableRepository

    SubscribedModelController(ItemCacheableRepository.SubscribedModelCacheableRepository subscribedModelCacheableRepository,
                             SubscribedCatalogueCacheableRepository subscribedCatalogueCacheableRepository) {
        super(subscribedModelCacheableRepository)
        this.subscribedModelCacheableRepository = subscribedModelCacheableRepository
        this.subscribedCatalogueCacheableRepository = subscribedCatalogueCacheableRepository
    }

    @Get(Paths.SUBSCRIBED_MODELS_ROUTE)
    ListResponse<SubscribedModel> listAll(@NonNull UUID subscribedCatalogueId) {
        accessControlService.checkAdministrator()
        ListResponse.from(subscribedModelCacheableRepository.findAllBySubscribedCatalogueId(subscribedCatalogueId))
    }

    @Post(Paths.SUBSCRIBED_MODELS_ROUTE)
    @Transactional
    SubscribedModel create(@NonNull UUID subscribedCatalogueId,@Body @NonNull SubscribedModel subscribedModel) {
        accessControlService.checkAdministrator()
        cleanBody(subscribedModel)
        updateCreationProperties(subscribedModel)
        SubscribedCatalogue subscribedCatalogue = subscribedCatalogueCacheableRepository.readById(subscribedCatalogueId)
        super.handleNotFoundError(subscribedCatalogue as Item, subscribedCatalogueId)
        subscribedModel.subscribedCatalogueId = subscribedCatalogue.id
        subscribedModelCacheableRepository.save(subscribedModel)
    }

}


