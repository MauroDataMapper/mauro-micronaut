package uk.ac.ox.softeng.mauro.service.federation

import uk.ac.ox.softeng.mauro.controller.Paths
import uk.ac.ox.softeng.mauro.controller.federation.client.FederationClient
import uk.ac.ox.softeng.mauro.domain.facet.federation.PublishedModel
import uk.ac.ox.softeng.mauro.domain.facet.federation.SubscribedCatalogue
import uk.ac.ox.softeng.mauro.domain.facet.federation.converter.SubscribedCatalogueConverter
import uk.ac.ox.softeng.mauro.domain.facet.federation.converter.SubscribedCatalogueConverterService
import uk.ac.ox.softeng.mauro.domain.facet.federation.response.SubscribedCataloguesPublishedModelsNewerVersions

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import jakarta.inject.Inject

import java.time.Instant

@CompileStatic
@Slf4j
class SubscribedCatalogueService {

    final FederationClient federationClient
    final SubscribedCatalogueConverterService subscribedCatalogueConverterService

    @Inject
    SubscribedCatalogueService(FederationClient federationClient, SubscribedCatalogueConverterService subscribedCatalogueConverterService) {
        this.federationClient = federationClient
        this.subscribedCatalogueConverterService = subscribedCatalogueConverterService
    }

    List<PublishedModel> getPublishedModels(SubscribedCatalogue subscribedCatalogue) {
        Map<String, Object> subscribedCatalogueModelsMap = federationClient.fetchFederatedClientDataAsMap(subscribedCatalogue, Paths.PUBLISHED_MODELS_ROUTE)
        getConverterForSubscribedCatalogue(subscribedCatalogue).toPublishedModels(subscribedCatalogueModelsMap).v2
    }


    SubscribedCataloguesPublishedModelsNewerVersions getNewerVersionsForPublishedModels(SubscribedCatalogue subscribedCatalogue, UUID subscribedModelId) {
        Map<String, Object> newerVersionsAsMap =
            federationClient.fetchFederatedClientDataAsMap(subscribedCatalogue, "$Paths.PUBLISHED_MODELS_ROUTE/$subscribedModelId$Paths.NEWER_VERSIONS")
        Tuple2<Instant, List<PublishedModel>> publishedModelsTuple = getConverterForSubscribedCatalogue(subscribedCatalogue).publishedModelsNewerVersions(newerVersionsAsMap)
        new SubscribedCataloguesPublishedModelsNewerVersions().tap {
            lastUpdated = publishedModelsTuple.v1 ?: Instant.now()
            newerPublishedModels = publishedModelsTuple.v2 ?: []
        }
    }



    byte[] getBytesResourceExport(SubscribedCatalogue subscribedCatalogue, String resourceUrl) {
        federationClient.clientSetup(subscribedCatalogue)
        federationClient.retrieveBytesFromClient(subscribedCatalogue, resourceUrl)
    }

    private SubscribedCatalogueConverter getConverterForSubscribedCatalogue(SubscribedCatalogue subscribedCatalogue) {
        subscribedCatalogueConverterService.getSubscribedCatalogueConverter(subscribedCatalogue.subscribedCatalogueType)
    }
}

