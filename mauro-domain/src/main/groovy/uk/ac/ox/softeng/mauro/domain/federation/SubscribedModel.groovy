package uk.ac.ox.softeng.mauro.domain.federation

import uk.ac.ox.softeng.mauro.domain.model.Item

import com.fasterxml.jackson.annotation.JsonAlias
import groovy.transform.AutoClone
import groovy.transform.CompileStatic
import groovy.transform.MapConstructor
import io.micronaut.core.annotation.Introspected
import io.micronaut.data.annotation.MappedEntity

import java.time.OffsetDateTime

/**
 * Subscribed Catalogue is created via UI
 */
@CompileStatic
@AutoClone
@Introspected
@MappedEntity(schema = 'federation')
@MapConstructor(includeSuperFields = true, includeSuperProperties = true, noArg = true)
class SubscribedModel extends Item {

    //The ID of the model on the remote (subscribed) catalogue
    String subscribedModelId
    String subscribedModelType
    //The folder that the model should be imported into
    UUID folderId
    //The last time that the model was last read from the remote (subscribed) catalogue
    OffsetDateTime lastRead
    //The ID of the model when imported into the local catalogue.
    UUID localModelId

    @JsonAlias(['subscribed_catalogue_id'])
    UUID subscribedCatalogueId

}
