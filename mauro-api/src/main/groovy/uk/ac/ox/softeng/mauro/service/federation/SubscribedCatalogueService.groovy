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
    final FederationClientConfiguration federationClientConfiguration

    @Inject
    SubscribedCatalogueService(FederationClient federationClient, SubscribedCatalogueConverterService subscribedCatalogueConverterService,
                               FederationClientConfiguration federationClientConfiguration) {
        this.federationClient = federationClient
        this.subscribedCatalogueConverterService = subscribedCatalogueConverterService
        this.federationClientConfiguration = federationClientConfiguration
    }

    List<PublishedModel> getPublishedModelsWithAuthority(SubscribedCatalogue subscribedCatalogue) {
        federationClient.initHttpClient(subscribedCatalogue, federationClientConfiguration)
        Map<String, Object> subscribedCatalogueModelsMap = federationClient.fetchFederatedClientDataAsMap(Paths.PUBLISHED_MODELS_ROUTE)
        getConverterForSubscribedCatalogue(subscribedCatalogue).toPublishedModels(subscribedCatalogueModelsMap).v2

    }

    private SubscribedCatalogueConverter getConverterForSubscribedCatalogue(SubscribedCatalogue subscribedCatalogue) {
        subscribedCatalogueConverterService.getSubscribedCatalogueConverter(subscribedCatalogue.subscribedCatalogueType)
    }

}

