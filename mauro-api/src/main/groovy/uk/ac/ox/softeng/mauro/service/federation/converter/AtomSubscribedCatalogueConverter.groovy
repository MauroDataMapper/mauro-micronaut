package uk.ac.ox.softeng.mauro.service.federation.converter

import uk.ac.ox.softeng.mauro.controller.federation.client.FederationClient
import uk.ac.ox.softeng.mauro.domain.authority.Authority
import uk.ac.ox.softeng.mauro.domain.facet.federation.MauroLink
import uk.ac.ox.softeng.mauro.domain.facet.federation.PublishedModel
import uk.ac.ox.softeng.mauro.domain.facet.federation.SubscribedCatalogue
import uk.ac.ox.softeng.mauro.domain.facet.federation.SubscribedCatalogueType

import groovy.util.logging.Slf4j
import groovy.xml.slurpersupport.GPathResult
import jakarta.inject.Singleton

import java.time.Instant

@Slf4j
@Singleton
class AtomSubscribedCatalogueConverter implements SubscribedCatalogueConverter {

    @Override
    boolean handles(SubscribedCatalogueType type) {
        type == SubscribedCatalogueType.ATOM
    }

    @Override
    Tuple2<Instant, List<PublishedModel>> publishedModelsNewerVersions(FederationClient federationClient, SubscribedCatalogue subscribedCatalogue, String publishedModelId , boolean older = false) {
        federationClient.clientSetup(subscribedCatalogue)
        //atom feed dosen't use additional context path
        GPathResult subscribedCatalogueModelsFeed = federationClient.getSubscribedCatalogueModelsFromAtomFeed()

        List<PublishedModel> publishedModels = subscribedCatalogueModelsFeed.entry.collect {convertEntryToPublishedModel(subscribedCatalogueModelsFeed, it)}
        PublishedModel publishedModel = publishedModels.find {it.modelId == publishedModelId}

        List<PublishedModel> newerVersions =
            publishedModels
                .findAll {
                    it.modelLabel == publishedModel?.modelLabel && ((!older && it.lastUpdated > publishedModel?.lastUpdated) ||
                                                                    (older && it.lastUpdated < publishedModel?.lastUpdated))
                }
                .sort {l, r ->
                    r.lastUpdated <=> l.lastUpdated ?:
                    l.modelLabel.compareToIgnoreCase(r.modelLabel) ?:
                    l.modelLabel <=> r.modelLabel ?:
                    l.modelId <=> r.modelId
                }

        Instant lastUpdated = newerVersions.collect {it.lastUpdated}.max()
        return new Tuple2(lastUpdated, newerVersions)
    }


    @Override
    Tuple2<Instant, List<PublishedModel>> getAuthorityAndPublishedModels(FederationClient federationClient, SubscribedCatalogue subscribedCatalogue, String path) {
        federationClient.clientSetup(subscribedCatalogue)
        GPathResult subscribedCatalogueModelsFeed = federationClient.getSubscribedCatalogueModelsFromAtomFeed()

        Authority subscribedAuthority = new Authority(label: subscribedCatalogueModelsFeed.author.name.text() ?: subscribedCatalogue.label,
                                                      url: subscribedCatalogueModelsFeed.author.uri.text() ?: subscribedCatalogue.url)

        List<PublishedModel> publishedModels = subscribedCatalogueModelsFeed.entry.collect {convertEntryToPublishedModel(subscribedCatalogueModelsFeed, it)}.sort {l, r ->
            r.lastUpdated <=> l.lastUpdated ?:
            l.modelLabel.compareToIgnoreCase(r.modelLabel) ?:
            l.modelLabel <=> r.modelLabel ?:
            l.modelId <=> r.modelId
        }
        return new Tuple2(subscribedAuthority, publishedModels)
    }

    static PublishedModel convertEntryToPublishedModel(GPathResult subscribedCatalogueModelsFeed, Object entry) {
        new PublishedModel().tap {
            modelId = entry.id
            modelLabel = entry.title
            if (entry.contentItemVersion.text()) modelVersionTag = entry.contentItemVersion.text()
            if (entry.updated.text()) lastUpdated = convert(entry.updated.text())
            if (entry.published.text()) datePublished = convert(entry.published.text())
            author = entry.author.name ?: subscribedCatalogueModelsFeed.author.name
            description = entry.summary
            links = entry.link.collect {link ->
                new MauroLink().tap{
                    url = link.@href.text()
                    contentType = link.@type
                }
            }
        }
    }
}


