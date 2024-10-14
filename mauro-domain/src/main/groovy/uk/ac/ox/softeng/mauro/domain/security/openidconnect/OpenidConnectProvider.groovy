package uk.ac.ox.softeng.mauro.domain.security.openidconnect

import groovy.transform.AutoClone
import groovy.transform.CompileStatic
import groovy.transform.MapConstructor
import io.micronaut.core.annotation.Introspected
import io.micronaut.core.annotation.NonNull
import io.micronaut.core.annotation.Nullable
import uk.ac.ox.softeng.mauro.domain.model.Item

@CompileStatic
@AutoClone
@Introspected
//@MappedEntity(schema = 'openidconnect')
@MapConstructor(includeSuperFields = true, includeSuperProperties = true, noArg = true)
class OpenidConnectProvider extends Item {
    UUID id
    String label
    Boolean standardProvider
    @Nullable
    String discoveryDocumentUrl
    @NonNull
    String clientId
    @NonNull
    String clientSecret

    //AuthorizationEndpointParameters authorizationEndpointParameters
    DiscoveryDocument discoveryDocument

    String imageUrl

    OpenidConnectProvider() {
       // this.authorizationEndpointParameters = new AuthorizationEndpointParameters()
    }
    OpenidConnectProvider(Map data){
        data.each {

        }
    }

}



