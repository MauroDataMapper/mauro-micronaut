package org.maurodata.authority

import org.maurodata.domain.authority.Authority
import org.maurodata.persistence.ContainerizedTest
import org.maurodata.testing.CommonDataSpec
import org.maurodata.web.ListResponse

import io.micronaut.runtime.EmbeddedApplication
import jakarta.inject.Inject

@ContainerizedTest
class BootstrapIntegrationSpec extends CommonDataSpec {

    @Inject
    EmbeddedApplication<?> application

    void 'On startup there should be authority with default authority set'() {
        when:
        ListResponse<Authority> authorityListResponse = authorityApi.list()
        then:
        authorityListResponse
        !authorityListResponse.items.isEmpty()
        Authority defaultAuthority = authorityListResponse.items.find{it.defaultAuthority == true}
        defaultAuthority.defaultAuthority == true
    }
}
