package uk.ac.ox.softeng.mauro.domain.security.openidconnect

import groovy.transform.AutoClone
import groovy.transform.CompileStatic
import groovy.transform.MapConstructor
import io.micronaut.core.annotation.Introspected
import io.micronaut.core.annotation.NonNull
import uk.ac.ox.softeng.mauro.domain.model.Item

@CompileStatic
@AutoClone
@Introspected
//@MappedEntity(schema = 'openidconnect')
@MapConstructor(includeSuperFields = true, includeSuperProperties = true, noArg = true)
class DiscoveryDocument extends Item {

    UUID id
    @NonNull
    String issuer
    @NonNull
    String authorizationEndpoint
    @NonNull
    String tokenEndpoint
    @NonNull
    String userinfoEndpoint
    @NonNull
    String endSessionEndpoint
    @NonNull
    String jwksUri

    @Override
    String getDomainType() {
        DiscoveryDocument.simpleName
    }
}