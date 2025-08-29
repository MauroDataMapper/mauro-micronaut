package org.maurodata.controller.bootstrap

import org.maurodata.domain.authority.Authority
import org.maurodata.service.core.AuthorityService

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import io.micronaut.context.event.ApplicationEventListener
import io.micronaut.core.annotation.Order
import io.micronaut.core.order.Ordered
import io.micronaut.discovery.event.ServiceReadyEvent
import jakarta.inject.Inject
import jakarta.inject.Singleton

@CompileStatic
@Slf4j
@Singleton
@Order(Ordered.HIGHEST_PRECEDENCE)
class BootstrapAuthority implements ApplicationEventListener<ServiceReadyEvent> {

@Inject
    AuthorityService authorityService

    @Override
    void onApplicationEvent(final ServiceReadyEvent event) {
        log.info("Running post processors: BootstrapAuthority")
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