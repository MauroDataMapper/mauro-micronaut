package uk.ac.ox.softeng.mauro.testing

import io.micronaut.runtime.EmbeddedApplication
import io.micronaut.runtime.server.EmbeddedServer
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import spock.lang.Specification
import jakarta.inject.Inject

@MicronautTest(transactional = false)
class SandboxSpec extends Specification {

    @Inject
    EmbeddedServer application

    void 'test it works'() {
        expect:
        application.running
    }

}
