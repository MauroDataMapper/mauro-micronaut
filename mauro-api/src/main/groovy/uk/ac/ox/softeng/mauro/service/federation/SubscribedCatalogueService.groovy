package uk.ac.ox.softeng.mauro.service.federation

import uk.ac.ox.softeng.mauro.ErrorHandler
import uk.ac.ox.softeng.mauro.Paths
import uk.ac.ox.softeng.mauro.controller.federation.client.FederationClient
import uk.ac.ox.softeng.mauro.domain.authority.Authority
import uk.ac.ox.softeng.mauro.domain.facet.federation.PublishedModel
import uk.ac.ox.softeng.mauro.domain.facet.federation.SubscribedCatalogue
import uk.ac.ox.softeng.mauro.domain.facet.federation.SubscribedCatalogueType
import uk.ac.ox.softeng.mauro.domain.facet.federation.response.SubscribedCataloguesPublishedModelsNewerVersions
import uk.ac.ox.softeng.mauro.service.core.AuthorityService
import uk.ac.ox.softeng.mauro.controller.federation.converter.SubscribedCatalogueConverter
import uk.ac.ox.softeng.mauro.controller.federation.converter.SubscribedCatalogueConverterService

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import io.micronaut.http.HttpStatus
import jakarta.inject.Inject

import java.time.Instant

@CompileStatic
@Slf4j
class SubscribedCatalogueService {

    final FederationClient federationClient
    final SubscribedCatalogueConverterService subscribedCatalogueConverterService
    final AuthorityService authorityService

    @Inject
    SubscribedCatalogueService(FederationClient federationClient, SubscribedCatalogueConverterService subscribedCatalogueConverterService, AuthorityService authorityService) {
        this.federationClient = federationClient
        this.subscribedCatalogueConverterService = subscribedCatalogueConverterService
        this.authorityService = authorityService
    }

    boolean validateRemote(SubscribedCatalogue subscribedCatalogue){
        boolean result
        Tuple2<Authority, List<PublishedModel>> remotePublishedModelsWithAuthority = getRemoteClientDataAsTuple(subscribedCatalogue, Paths.PUBLISHED_MODELS_ROUTE)
        Authority defaultAuthority = authorityService.getDefaultAuthority()
        // For Mauro JSON catalogues, check that the remote catalogue has a name (Authority label)
        // For both Mauro JSON and Atom catalogues, check that the publishedModels list exists, however this may be empty
        if ((subscribedCatalogue.subscribedCatalogueType == SubscribedCatalogueType.MAURO_JSON && !remotePublishedModelsWithAuthority.v1.label) ||
            remotePublishedModelsWithAuthority.v2 == null) {
            ErrorHandler.handleError(HttpStatus.UNPROCESSABLE_ENTITY, 'Invalid subscription')
        }
        if (remotePublishedModelsWithAuthority.v1.label == defaultAuthority.label && remotePublishedModelsWithAuthority.v1.url ==
            defaultAuthority.url) {
            ErrorHandler.handleError(HttpStatus.UNPROCESSABLE_ENTITY, 'Invalid subscription -check authority ')
        }
        result = true
        result
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

    private SubscribedCatalogueConverter getConverterForSubscribedCatalogue(SubscribedCatalogue subscribedCatalogue) {
        subscribedCatalogueConverterService.getSubscribedCatalogueConverter(subscribedCatalogue.subscribedCatalogueType)
    }

    private Tuple2<Authority, List<PublishedModel>> getRemoteClientDataAsTuple(SubscribedCatalogue subscribedCatalogue, String path) {
        getConverterForSubscribedCatalogue(subscribedCatalogue).getAuthorityAndPublishedModels(federationClient, subscribedCatalogue, path)
    }

}

