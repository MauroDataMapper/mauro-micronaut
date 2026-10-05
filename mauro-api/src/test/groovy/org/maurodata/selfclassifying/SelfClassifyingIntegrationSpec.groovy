package org.maurodata.selfclassifying

import org.maurodata.api.Paths
import org.maurodata.persistence.ContainerizedTest
import org.maurodata.plugin.MauroPluginDTO

import org.maurodata.testing.CommonDataSpec

import io.micronaut.core.type.Argument
import io.micronaut.http.HttpRequest
import jakarta.inject.Singleton

@ContainerizedTest
@Singleton
class SelfClassifyingIntegrationSpec extends CommonDataSpec {

    void 'classifying plugins only includes plugins with classifiers'() {
        when:
        List<Map> providers = lowLevelApi.client.toBlocking().retrieve(
            HttpRequest.GET('/api/selfClassifier/providers').tap {
                lowLevelApi.addHeaders(it)
            },
            Argument.listOf(Map))

        then:
        providers.isEmpty()

        when:
        providers = lowLevelApi.client.toBlocking().retrieve(
            HttpRequest.GET("/api/selfClassifier/providers?classifierNamespace=org.maurodata.database").tap {
                lowLevelApi.addHeaders(it)
            },
            Argument.listOf(Map))

        then:
        providers.isEmpty()

        when:
        providers = lowLevelApi.client.toBlocking().retrieve(
            HttpRequest.GET("/api/selfClassifier/providers?classifierNamespace=org.maurodata.database&classifierLabel=Table").tap {
                lowLevelApi.addHeaders(it)
            },
            Argument.listOf(Map))

        then:
        providers.isEmpty()

        when:
        providers = lowLevelApi.client.toBlocking().retrieve(
            HttpRequest.GET("/api/selfClassifier/providers?pluginKind=Profile").tap {
                lowLevelApi.addHeaders(it)
            },
            Argument.listOf(Map))

        then:
        providers.isEmpty()
    }
}
