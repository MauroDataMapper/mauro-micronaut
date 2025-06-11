package org.maurodata.testing

import org.maurodata.persistence.ContainerizedTest

import io.micronaut.runtime.EmbeddedApplication
import spock.lang.Specification
import jakarta.inject.Inject

@ContainerizedTest
class SandboxSpec extends Specification {

    @Inject
    EmbeddedApplication<?> application

    void 'test it works'() {
        expect:
        application.running
    }

}
