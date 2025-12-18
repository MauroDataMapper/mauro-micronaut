package org.maurodata.api.security.openidprovider

import groovy.transform.CompileStatic

@CompileStatic
class OpenidConnectProvider {
    UUID id
    String label
    boolean standardProvider
    String authorizationEndpoint
    String imageUrl

    OpenidConnectProvider(String openidProviderId, String label, Boolean standardProvider,
                          String authorizationEndpoint, String imageUrl) {
        if (openidProviderId != null) this.id = UUID.fromString(openidProviderId)
        this.label = label
        if (standardProvider != null) this.standardProvider = standardProvider
        this.authorizationEndpoint = authorizationEndpoint
        this.imageUrl = imageUrl
    }
}
