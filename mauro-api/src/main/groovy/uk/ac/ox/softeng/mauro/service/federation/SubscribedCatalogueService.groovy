package uk.ac.ox.softeng.mauro.service.federation

import uk.ac.ox.softeng.mauro.controller.Paths
import uk.ac.ox.softeng.mauro.controller.federation.client.FederationClient
import uk.ac.ox.softeng.mauro.controller.federation.client.FederationClientConfiguration
import uk.ac.ox.softeng.mauro.domain.authority.Authority
import uk.ac.ox.softeng.mauro.domain.federation.PublishedModel
import uk.ac.ox.softeng.mauro.domain.federation.SubscribedCatalogue
import uk.ac.ox.softeng.mauro.domain.federation.converter.SubscribedCatalogueConverter
import uk.ac.ox.softeng.mauro.domain.federation.converter.SubscribedCatalogueConverterService

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import jakarta.inject.Inject

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
       getPublishedModelsWithAuthority(subscribedCatalogue).v2
    }


    protected Tuple2<Authority, List<PublishedModel>> getPublishedModelsWithAuthority(SubscribedCatalogue subscribedCatalogue) {
        federationClient.initHttpClient(subscribedCatalogue)
        Map<String, Object> subscribedCatalogueModelsMap = federationClient.fetchFederatedClientDataAsMap(Paths.PUBLISHED_MODELS_ROUTE)
        getConverterForSubscribedCatalogue(subscribedCatalogue).toPublishedModels(subscribedCatalogueModelsMap)
    }

    private SubscribedCatalogueConverter getConverterForSubscribedCatalogue(SubscribedCatalogue subscribedCatalogue) {
        subscribedCatalogueConverterService.getSubscribedCatalogueConverter(subscribedCatalogue.subscribedCatalogueType)
    }

    byte[] getBytesResourceExport(SubscribedCatalogue subscribedCatalogue,String resourceUrl) {
        federationClient.initHttpClient(subscribedCatalogue)
        federationClient.retrieveBytesFromClient(resourceUrl)
    }
}

