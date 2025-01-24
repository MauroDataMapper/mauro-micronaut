package uk.ac.ox.softeng.mauro.domain.federation

import uk.ac.ox.softeng.mauro.domain.model.InstantConverter
import uk.ac.ox.softeng.mauro.domain.model.Item
import uk.ac.ox.softeng.mauro.domain.security.SecurableResource

import com.fasterxml.jackson.annotation.JsonAlias
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
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
    @JsonProperty('api_key')
    String apiKey
    @Nullable
    @JsonProperty('token_url')
    String tokenUrl
    @Nullable
    @JsonProperty('client_id')
    String clientId
    @Nullable
    @JsonProperty('client_secret')
    String clientSecret
    @Nullable
    @JsonProperty('access_token')
    String accessToken
    @Nullable
    @JsonProperty('access_token_expiry_time')
    Instant accessTokenExpiryTime

    @DateUpdated
    @JsonDeserialize(converter = InstantConverter)
    @JsonAlias(['last_read'])
    Instant lastRead

    @JsonAlias(['subscribed_models'])
    @Relation(value = Relation.Kind.ONE_TO_MANY, mappedBy = 'subscribedCatalogue', cascade = Relation.Cascade.ALL)
    List<SubscribedModel> subscribedModels = []

    @Transient
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    String domainType = this.class.simpleName

}

