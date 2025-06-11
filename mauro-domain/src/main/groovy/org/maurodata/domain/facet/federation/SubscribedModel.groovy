package org.maurodata.domain.facet.federation


import org.maurodata.domain.model.Item
import org.maurodata.domain.security.SecurableResource

import com.fasterxml.jackson.annotation.JsonAlias
import groovy.transform.AutoClone
import groovy.transform.CompileStatic
import groovy.transform.MapConstructor
import io.micronaut.core.annotation.Introspected
import io.micronaut.core.annotation.NonNull
import io.micronaut.data.annotation.DateUpdated
import io.micronaut.data.annotation.MappedEntity
import jakarta.validation.constraints.NotNull

import java.time.Instant

@CompileStatic
@AutoClone
@Introspected
@MappedEntity(schema = 'federation')
@MapConstructor(includeSuperFields = true, includeSuperProperties = true, noArg = true)
class SubscribedModel extends Item implements SecurableResource{
    @NotNull
    SubscribedCatalogue subscribedCatalogue

    //The ID of the model on the remote (subscribed) catalogue
    @NonNull
    String subscribedModelId

    //The folder that the model should be imported into
    @NonNull
    UUID folderId

    String subscribedModelType

    @JsonAlias(['readable_by_everyone'])
    @NonNull
    Boolean readableByEveryone = false

    @JsonAlias(['readable_by_authenticated_users'])
    @NonNull
    Boolean readableByAuthenticatedUsers = false

    @DateUpdated
    @JsonAlias(['last_read'])
    Instant lastRead

    //The ID of the model when imported into the local catalogue.
    @NonNull
    UUID localModelId

}
