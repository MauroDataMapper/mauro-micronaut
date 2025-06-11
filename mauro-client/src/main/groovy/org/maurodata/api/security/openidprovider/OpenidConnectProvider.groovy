package org.maurodata.api.security.openidprovider

class OpenidConnectProvider {
    UUID openidProviderId
    String label
    boolean standardProvider
    String authorizationEndpoint
    String imageUrl

    OpenidConnectProvider(String openidProviderId, String label, Boolean standardProvider,
                          String authorizationEndpoint, String imageUrl) {
        if(openidProviderId!=null) this.openidProviderId = UUID.fromString(openidProviderId)
        this.label = label
        if(standardProvider!=null) this.standardProvider = standardProvider
        this.authorizationEndpoint = authorizationEndpoint
        this.imageUrl = imageUrl
    }
}
