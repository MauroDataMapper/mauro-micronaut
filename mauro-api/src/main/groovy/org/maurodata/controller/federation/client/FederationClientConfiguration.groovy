package org.maurodata.controller.federation.client

import io.micronaut.context.annotation.ConfigurationProperties
import io.micronaut.context.annotation.Replaces
import io.micronaut.http.client.HttpClientConfiguration
import io.micronaut.runtime.ApplicationConfiguration

@ConfigurationProperties(PREFIX)
//@Replaces(HttpClientConfiguration)
class FederationClientConfiguration extends HttpClientConfiguration {
    static final String PREFIX = "micronaut.federation.client";
    static final String API_KEY_HEADER = 'apiKey'
    static final String API_PATH = 'api'

    FederationClientConnectionPoolConfiguration connectionPoolConfiguration;

    FederationClientConfiguration(ApplicationConfiguration applicationConfiguration,
                                  FederationClientConnectionPoolConfiguration connectionPoolConfiguration) {
        super(applicationConfiguration)
        this.connectionPoolConfiguration = connectionPoolConfiguration
    }

    @Override
    ConnectionPoolConfiguration getConnectionPoolConfiguration() {
        connectionPoolConfiguration
    }

    @ConfigurationProperties(PREFIX)
    @Replaces(HttpClientConfiguration.ConnectionPoolConfiguration)
    static class FederationClientConnectionPoolConfiguration extends HttpClientConfiguration.ConnectionPoolConfiguration {
    }
}
