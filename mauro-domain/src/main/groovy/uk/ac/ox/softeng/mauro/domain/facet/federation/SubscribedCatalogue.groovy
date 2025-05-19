package uk.ac.ox.softeng.mauro.domain.facet.federation


import uk.ac.ox.softeng.mauro.domain.model.Item
import uk.ac.ox.softeng.mauro.domain.security.SecurableResource

import com.fasterxml.jackson.annotation.JsonAlias
import com.fasterxml.jackson.annotation.JsonProperty
import groovy.transform.AutoClone
import groovy.transform.CompileStatic
import groovy.transform.MapConstructor
import io.micronaut.core.annotation.Introspected
import io.micronaut.core.annotation.NonNull
import io.micronaut.core.annotation.Nullable
import io.micronaut.data.annotation.DateUpdated
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
class SubscribedCatalogue extends Item implements SecurableResource{
    @NonNull
    String url

    @NonNull
    String label

    @JsonAlias(['refresh_period'])
    int refreshPeriod

    @NonNull
    @JsonAlias(['subscribed_catalogue_type'])
    SubscribedCatalogueType subscribedCatalogueType

    @JsonAlias(['subscribed_catalogue_authentication_type'])
    @NonNull
    SubscribedCatalogueAuthenticationType subscribedCatalogueAuthenticationType

    @JsonAlias(['readable_by_everyone'])
    @NonNull
    Boolean readableByEveryone = false

    @JsonAlias(['readable_by_authenticated_users'])
    @NonNull
    Boolean readableByAuthenticatedUsers = false

    @Nullable
    @JsonAlias('api_key')
    String apiKey
    @Nullable
    @JsonAlias('token_url')
    String tokenUrl
    @Nullable
    @JsonAlias('client_id')
    String clientId
    @Nullable
    @JsonAlias('client_secret')
    String clientSecret
    @Nullable
    @JsonAlias('access_token')
    String accessToken
    @Nullable
    @JsonAlias('access_token_expiry_time')
    Instant accessTokenExpiryTime

    @DateUpdated
    @JsonAlias(['last_read'])
    Instant lastRead

    @JsonAlias(['subscribed_models'])
    @Relation(value = Relation.Kind.ONE_TO_MANY, mappedBy = 'subscribedCatalogue')
    List<SubscribedModel> subscribedModels = []

    @Transient
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    String domainType = this.class.simpleName

}

