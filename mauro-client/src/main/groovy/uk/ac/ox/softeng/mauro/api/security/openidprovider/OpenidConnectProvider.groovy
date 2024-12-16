package uk.ac.ox.softeng.mauro.api.security.openidprovider

class OpenidConnectProvider {
    UUID openidProviderId
    String label
    boolean standardProvider
    String authorizationEndpoint
    String imageUrl

    OpenidConnectProvider(String openidProviderId, String label, boolean standardProvider,
                          String authorizationEndpoint, String imageUrl) {
        this.openidProviderId = UUID.fromString(openidProviderId)
        this.label = label
        this.standardProvider = standardProvider
        this.authorizationEndpoint = authorizationEndpoint
        this.imageUrl = imageUrl
    }
}
