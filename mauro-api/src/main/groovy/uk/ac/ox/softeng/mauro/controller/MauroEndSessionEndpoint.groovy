package uk.ac.ox.softeng.mauro.controller

import groovy.transform.CompileStatic
import io.micronaut.http.HttpRequest
import io.micronaut.security.authentication.Authentication
import io.micronaut.security.oauth2.client.OpenIdProviderMetadata
import io.micronaut.security.oauth2.endpoint.endsession.request.EndSessionEndpoint
import jakarta.inject.Inject
import jakarta.inject.Singleton

@Singleton
@CompileStatic
class MauroEndSessionEndpoint implements EndSessionEndpoint {
    @Inject
    OpenIdProviderMetadata openIdProviderMetadata

    MauroEndSessionEndpoint(OpenIdProviderMetadata openIdProviderMetadata) {
        this.openIdProviderMetadata = openIdProviderMetadata
    }

    @Override
    String getUrl(HttpRequest<?> originating, Authentication authentication) {
        return openIdProviderMetadata.endSessionEndpoint
    }
}
