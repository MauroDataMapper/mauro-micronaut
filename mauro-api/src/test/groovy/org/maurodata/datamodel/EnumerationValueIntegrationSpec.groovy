package org.maurodata.datamodel


import jakarta.inject.Singleton
import org.maurodata.persistence.ContainerizedTest
import org.maurodata.testing.CommonDataSpec

@ContainerizedTest
@Singleton
class EnumerationValueIntegrationSpec extends CommonDataSpec {

    void 'get enumerationvalues doi endpoint - returns null'(){
        when:
        Map listResponse = enumerationValueApi.doi(UUID.randomUUID())
        then:
        !listResponse
    }
}