package uk.ac.ox.softeng.mauro.controller

import uk.ac.ox.softeng.mauro.domain.authority.Authority
import uk.ac.ox.softeng.mauro.service.core.AuthorityService

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import io.micronaut.discovery.event.ServiceReadyEvent
import io.micronaut.runtime.event.annotation.EventListener
import io.micronaut.scheduling.annotation.Async
import jakarta.inject.Inject
import jakarta.inject.Singleton

@CompileStatic
@Slf4j
@Singleton
class BootstrapAuthority  {

    @Inject
    AuthorityService authorityService

    @EventListener
    @Async
    void onApplicationEvent(final ServiceReadyEvent event) {
        if (!authorityService.getDefaultAuthority()) {
            createDefaultAuthority()
        }
    }

    private Authority createDefaultAuthority() {
        log.debug("Creating default authority ")
        Authority authority = new Authority().tap {
            label = 'Mauro Sandbox'
            url = 'http://localhost:8088'
            readableByEveryone = true
            readableByAuthenticatedUsers = true
            defaultAuthority = true
        }
        if (!authorityService.getDefaultAuthority()) {
            log.debug("Persisting default authority")
            authorityService.create(authority)
        }
    }


}