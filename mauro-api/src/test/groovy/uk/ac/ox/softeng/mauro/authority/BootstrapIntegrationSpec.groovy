package uk.ac.ox.softeng.mauro.authority

import uk.ac.ox.softeng.mauro.domain.authority.Authority
import uk.ac.ox.softeng.mauro.persistence.ContainerizedTest
import uk.ac.ox.softeng.mauro.testing.CommonDataSpec
import uk.ac.ox.softeng.mauro.web.ListResponse

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
