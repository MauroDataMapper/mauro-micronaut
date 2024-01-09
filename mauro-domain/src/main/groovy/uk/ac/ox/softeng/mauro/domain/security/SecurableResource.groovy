package uk.ac.ox.softeng.mauro.domain.security

import groovy.transform.CompileStatic
import io.micronaut.core.annotation.Introspected
import io.micronaut.data.annotation.Id

@CompileStatic
@Introspected
trait SecurableResource {

    abstract UUID getId()

    abstract Boolean getReadableByEveryone()

    abstract Boolean getReadableByAuthenticatedUsers()
}