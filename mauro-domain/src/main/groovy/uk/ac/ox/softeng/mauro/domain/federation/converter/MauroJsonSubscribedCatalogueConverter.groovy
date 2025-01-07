package uk.ac.ox.softeng.mauro.domain.federation.converter

import uk.ac.ox.softeng.mauro.domain.authority.Authority
import uk.ac.ox.softeng.mauro.domain.federation.MauroLink
import uk.ac.ox.softeng.mauro.domain.federation.PublishedModel
import uk.ac.ox.softeng.mauro.domain.federation.SubscribedCatalogueType
import uk.ac.ox.softeng.mauro.domain.model.version.ModelVersion

import groovy.util.logging.Slf4j
import jakarta.inject.Singleton

import java.time.Instant
import java.time.OffsetDateTime
import java.time.format.DateTimeParseException

@Slf4j
@Singleton
class MauroJsonSubscribedCatalogueConverter implements SubscribedCatalogueConverter {

    @Override
    boolean handles(SubscribedCatalogueType type) {
        type == SubscribedCatalogueType.MAURO_JSON
    }

    @Override
    Tuple2<Authority, List<PublishedModel>> toPublishedModels(Map<String, Object> subscribedCatalogueModelsMap) {
        Authority subscribedAuthority = new Authority().tap {
            if (subscribedCatalogueModelsMap.authority) {
                label = subscribedCatalogueModelsMap.authority.label ?: null
                url = subscribedCatalogueModelsMap.authority.url ?: null
            }
        }

        List<PublishedModel> publishedModels = (subscribedCatalogueModelsMap.publishedModels as List<Map<String, Object>>).collect {convertEntryToPublishedModel(it)}
        return new Tuple2(subscribedAuthority, publishedModels)
    }

    protected PublishedModel convertEntryToPublishedModel(Map<String, Object> entry) {
        PublishedModel model = new PublishedModel().tap {
            modelId = entry.modelId
            modelLabel = entry.label
            modelVersion = ModelVersion.from(entry.version)
            modelVersionTag = entry.modelVersionTag
            modelType = entry.modelType
            if (entry.lastUpdated) lastUpdated =convert(entry.lastUpdated as String)
            if (entry.dateCreated) dateCreated = convert(entry.dateCreated as String)
            if (entry.datePubished) datePublished = convert(entry.datePublished as String)
            author = entry.author
            description = entry.description
            if (entry.links) links = entry.links.collect {link -> new MauroLink(link.url, link.contentType)}
        }
        return model
    }

    static Instant convert(String value) {
        try {
            return OffsetDateTime.parse(value).toInstant()
        } catch (DateTimeParseException ignored) {
            // if timezone is missing, assume UTC (used for deserialising JSON from Postgres)
            return OffsetDateTime.parse(value + 'Z').toInstant()
        }
    }
}

