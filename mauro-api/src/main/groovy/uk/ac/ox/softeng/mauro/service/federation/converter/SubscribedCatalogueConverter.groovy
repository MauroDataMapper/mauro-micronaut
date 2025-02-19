package uk.ac.ox.softeng.mauro.service.federation.converter

import uk.ac.ox.softeng.mauro.controller.federation.client.FederationClient
import uk.ac.ox.softeng.mauro.domain.authority.Authority
import uk.ac.ox.softeng.mauro.domain.facet.federation.PublishedModel
import uk.ac.ox.softeng.mauro.domain.facet.federation.SubscribedCatalogue
import uk.ac.ox.softeng.mauro.domain.facet.federation.SubscribedCatalogueType

import java.time.Instant
import java.time.OffsetDateTime
import java.time.format.DateTimeParseException

trait SubscribedCatalogueConverter {

    abstract boolean handles(SubscribedCatalogueType type)

    abstract Tuple2<Instant, List<PublishedModel>> publishedModelsNewerVersions(FederationClient federationClient, SubscribedCatalogue subscribedCatalogue, String publishedModelId)

    abstract Tuple2<Authority, List<PublishedModel>> getAuthorityAndPublishedModels(FederationClient federationClient, SubscribedCatalogue subscribedCatalogue,
                                                                                   String requestPath)

    static Instant convert(String value) {
        try {
            return OffsetDateTime.parse(value).toInstant()
        } catch (DateTimeParseException ignored) {
            // if timezone is missing, assume UTC (used for deserialising JSON from Postgres)
            return OffsetDateTime.parse(value + 'Z').toInstant()
        }
    }
}
