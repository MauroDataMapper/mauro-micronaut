package org.maurodata.controller.federation.converter

import org.maurodata.controller.federation.client.FederationClient
import org.maurodata.domain.authority.Authority
import org.maurodata.domain.facet.federation.PublishedModel
import org.maurodata.domain.facet.federation.SubscribedCatalogue
import org.maurodata.domain.facet.federation.SubscribedCatalogueType

import groovy.transform.CompileStatic

import java.time.Instant
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.format.DateTimeParseException

@CompileStatic
trait SubscribedCatalogueConverter {

    abstract boolean handles(SubscribedCatalogueType type)

    abstract Tuple2<Instant, List<PublishedModel>> publishedModelsNewerVersions(FederationClient federationClient, SubscribedCatalogue subscribedCatalogue, String publishedModelId)

    abstract Tuple2<Authority, List<PublishedModel>> getAuthorityAndPublishedModels(FederationClient federationClient, SubscribedCatalogue subscribedCatalogue,
                                                                                   String requestPath)

    static Instant convert(String value) {
        try {
            return OffsetDateTime.parse(value).toInstant()
        } catch (DateTimeParseException ignored) {
            return LocalDateTime.parse(value).toInstant(OffsetDateTime.now().getOffset())
        }
    }
}
