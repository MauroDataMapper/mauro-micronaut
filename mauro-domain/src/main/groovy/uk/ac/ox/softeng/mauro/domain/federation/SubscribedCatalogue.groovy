package uk.ac.ox.softeng.mauro.domain.federation

import uk.ac.ox.softeng.mauro.domain.model.Item

import com.fasterxml.jackson.annotation.JsonAlias
import com.fasterxml.jackson.annotation.JsonProperty
import groovy.transform.AutoClone
import groovy.transform.CompileStatic
import groovy.transform.MapConstructor
import io.micronaut.core.annotation.Introspected
import io.micronaut.core.annotation.NonNull
import io.micronaut.core.annotation.Nullable
import io.micronaut.data.annotation.MappedEntity
import io.micronaut.data.annotation.Relation
import jakarta.persistence.Transient

import java.time.Instant

/**
 * Subscribed Catalogue is created via UI
 */
@CompileStatic
@AutoClone
@Introspected
@MappedEntity(schema = 'federation')
@MapConstructor(includeSuperFields = true, includeSuperProperties = true, noArg = true)
class SubscribedCatalogue extends Item {
    @NonNull
    String url

    // connection timeout in minutes
    @JsonProperty("connectionTimeout")
    int connectTimeout
    Instant lastRead
    @NonNull
    String label

    @JsonAlias(['refresh_period'])
    @NonNull
    int refreshPeriod

    @NonNull
    @JsonAlias(['subscribed_catalogue_type'])
    SubscribedCatalogueType subscribedCatalogueType

    @JsonAlias(['subscribed_catalogue_authentication_type'])
    @NonNull
    SubscribedCatalogueAuthenticationType subscribedCatalogueAuthenticationType

    @Nullable
    String apiKey
    @Nullable
    String tokenUrl
    @Nullable
    String clientId
    @Nullable
    String clientSecret
    @Nullable
    String accessToken
    @Nullable
    Instant accessTokenExpiryTime

    @JsonAlias(['subscribed_catalogue'])
    @Relation(value = Relation.Kind.ONE_TO_MANY, mappedBy = 'subscribed_catalogue_id', cascade = Relation.Cascade.ALL)
    List<SubscribedModel> subscribedModels = []

    @Transient
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    String domainType = this.class.simpleName

}

