package org.maurodata.controller.federation.converter

import org.maurodata.api.Paths
import org.maurodata.controller.federation.client.FederationClient
import org.maurodata.domain.authority.Authority
import org.maurodata.domain.facet.federation.MauroLink
import org.maurodata.domain.facet.federation.PublishedModel
import org.maurodata.domain.facet.federation.SubscribedCatalogue
import org.maurodata.domain.facet.federation.SubscribedCatalogueType
import org.maurodata.domain.model.version.ModelVersion

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import jakarta.inject.Singleton

import java.time.Instant

@CompileStatic
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
                label = subscribedCatalogueModelsMap.authority['label'] as String?: null
                url = subscribedCatalogueModelsMap.authority['url'] as String?: null
            }
        }
        List<PublishedModel> publishedModels = (subscribedCatalogueModelsMap.publishedModels as List<Map<String, Object>>).collect {convertEntryToPublishedModel(it)}
        return new Tuple2(subscribedAuthority, publishedModels)
    }

    @Override
    Tuple2<Instant, List<PublishedModel>> publishedModelsNewerVersions(FederationClient federationClient, SubscribedCatalogue subscribedCatalogue, String publishedModelId){
        federationClient.clientSetup(subscribedCatalogue)
        Map<String, Object> newerVersionsMap = federationClient.fetchFederatedClientDataAsMap(subscribedCatalogue, "$Paths.PUBLISHED_MODELS/$publishedModelId/newerVersions")

        Instant lastUpdated = Instant.parse(newerVersionsMap.lastUpdated as CharSequence)
        List<PublishedModel> newerVersions = (newerVersionsMap.newerPublishedModels as List<Map<String, Object>>).collect {convertEntryToPublishedModel(it)}
        return new Tuple2(lastUpdated, newerVersions)
    }

    protected PublishedModel convertEntryToPublishedModel(Map<String, Object> entry) {
        PublishedModel model = new PublishedModel().tap {
            modelId = entry.modelId
            modelLabel = entry.label
            modelVersion = ModelVersion.from(entry.version as String)
            modelVersionTag = entry.modelVersionTag
            modelType = entry.modelType
            if (entry.lastUpdated) lastUpdated = convert(entry.lastUpdated as String)
            if (entry.dateCreated) dateCreated = convert(entry.dateCreated as String)
            if (entry.datePublished) datePublished = convert(entry.datePublished as String)
            author = entry.author
            description = entry.description
            if (entry.links) links = entry.links.collect {link -> new MauroLink(link['url'] as String, link['contentType'] as String)}
        }
        return model
    }

}

