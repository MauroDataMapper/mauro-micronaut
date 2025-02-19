package uk.ac.ox.softeng.mauro.service.federation

import uk.ac.ox.softeng.mauro.controller.Paths
import uk.ac.ox.softeng.mauro.controller.federation.client.FederationClient
import uk.ac.ox.softeng.mauro.domain.authority.Authority
import uk.ac.ox.softeng.mauro.domain.facet.federation.PublishedModel
import uk.ac.ox.softeng.mauro.domain.facet.federation.SubscribedCatalogue
import uk.ac.ox.softeng.mauro.domain.facet.federation.response.SubscribedCataloguesPublishedModelsNewerVersions
import uk.ac.ox.softeng.mauro.service.federation.converter.SubscribedCatalogueConverter
import uk.ac.ox.softeng.mauro.service.federation.converter.SubscribedCatalogueConverterService

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
        getRemoteClientDataAsTuple(subscribedCatalogue, Paths.PUBLISHED_MODELS_ROUTE).v2
    }


    SubscribedCataloguesPublishedModelsNewerVersions getNewerVersionsForPublishedModels(SubscribedCatalogue subscribedCatalogue, String publishedModelId) {
        Tuple2<Instant, List<PublishedModel>> publishedModelsNewerVersionsTuple = getConverterForSubscribedCatalogue(subscribedCatalogue).publishedModelsNewerVersions(federationClient, subscribedCatalogue, publishedModelId)

        new SubscribedCataloguesPublishedModelsNewerVersions().tap {
            lastUpdated = publishedModelsNewerVersionsTuple.v1 ?: Instant.now() as Instant
            newerPublishedModels = publishedModelsNewerVersionsTuple.v2 ?: []
        }
    }

    byte[] getBytesResourceExport(SubscribedCatalogue subscribedCatalogue, String resourceUrl) {
        federationClient.clientSetup(subscribedCatalogue)
        federationClient.retrieveBytesFromClient(subscribedCatalogue, resourceUrl)
    }

    Authority getAuthority(SubscribedCatalogue subscribedCatalogue) {
        getRemoteClientDataAsTuple(subscribedCatalogue, Paths.PUBLISHED_MODELS_ROUTE).v1
    }

    private SubscribedCatalogueConverter getConverterForSubscribedCatalogue(SubscribedCatalogue subscribedCatalogue) {
        subscribedCatalogueConverterService.getSubscribedCatalogueConverter(subscribedCatalogue.subscribedCatalogueType)
    }

    private Tuple2<Authority, List<PublishedModel>> getRemoteClientDataAsTuple(SubscribedCatalogue subscribedCatalogue, String path) {
        getConverterForSubscribedCatalogue(subscribedCatalogue).getAuthorityAndPublishedModels(federationClient, subscribedCatalogue, path)
    }

}

