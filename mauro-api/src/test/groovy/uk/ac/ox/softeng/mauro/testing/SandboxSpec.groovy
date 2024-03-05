package uk.ac.ox.softeng.mauro.testing

import uk.ac.ox.softeng.mauro.persistence.ContainerizedTest

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
