package uk.ac.ox.softeng.mauro.service.federation.converter

import uk.ac.ox.softeng.mauro.controller.Paths
import uk.ac.ox.softeng.mauro.controller.federation.client.FederationClient
import uk.ac.ox.softeng.mauro.domain.authority.Authority
import uk.ac.ox.softeng.mauro.domain.facet.federation.MauroLink
import uk.ac.ox.softeng.mauro.domain.facet.federation.PublishedModel
import uk.ac.ox.softeng.mauro.domain.facet.federation.SubscribedCatalogue
import uk.ac.ox.softeng.mauro.domain.facet.federation.SubscribedCatalogueType
import uk.ac.ox.softeng.mauro.domain.model.version.ModelVersion

import groovy.util.logging.Slf4j
import jakarta.inject.Singleton

import java.time.Instant

@Slf4j
@Singleton
class MauroJsonSubscribedCatalogueConverter implements SubscribedCatalogueConverter {

    @Override
    boolean handles(SubscribedCatalogueType type) {
        type == SubscribedCatalogueType.MAURO_JSON
    }

    @Override
    Tuple2<Instant, List<PublishedModel>> getAuthorityAndPublishedModels(FederationClient federationClient, SubscribedCatalogue subscribedCatalogue, String path) {
        federationClient.clientSetup(subscribedCatalogue)
        Map<String, Object> subscribedCatalogueModelsMap = federationClient.fetchFederatedClientDataAsMap(subscribedCatalogue, path)
        Authority subscribedAuthority = new Authority().tap {
            if (subscribedCatalogueModelsMap.authority) {
                label = subscribedCatalogueModelsMap.authority.label ?: null
                url = subscribedCatalogueModelsMap.authority.url ?: null
            }
        }
        List<PublishedModel> publishedModels = (subscribedCatalogueModelsMap.publishedModels as List<Map<String, Object>>).collect {convertEntryToPublishedModel(it)}
        return new Tuple2(subscribedAuthority, publishedModels)
    }

    @Override
    Tuple2<Instant, List<PublishedModel>> publishedModelsNewerVersions(FederationClient federationClient, SubscribedCatalogue subscribedCatalogue, String publishedModelId){
        federationClient.clientSetup(subscribedCatalogue)
        Map<String, Object> newerVersionsMap = federationClient.fetchFederatedClientDataAsMap(subscribedCatalogue, "$Paths.PUBLISHED_MODELS_ROUTE/$publishedModelId$Paths.NEWER_VERSIONS")

        Instant lastUpdated = Instant.parse(newerVersionsMap.lastUpdated as CharSequence)
        List<PublishedModel> newerVersions = (newerVersionsMap.newerPublishedModels as List<Map<String, Object>>).collect {convertEntryToPublishedModel(it)}
        return new Tuple2(lastUpdated, newerVersions)
    }

    protected PublishedModel convertEntryToPublishedModel(Map<String, Object> entry) {
        PublishedModel model = new PublishedModel().tap {
            modelId = entry.modelId
            modelLabel = entry.label
            modelVersion = ModelVersion.from(entry.version)
            modelVersionTag = entry.modelVersionTag
            modelType = entry.modelType
            if (entry.lastUpdated) lastUpdated = convert(entry.lastUpdated as String)
            if (entry.dateCreated) dateCreated = convert(entry.dateCreated as String)
            if (entry.datePubished) datePublished = convert(entry.datePublished as String)
            author = entry.author
            description = entry.description
            if (entry.links) links = entry.links.collect {link -> new MauroLink(link.url, link.contentType)}
        }
        return model
    }

}

