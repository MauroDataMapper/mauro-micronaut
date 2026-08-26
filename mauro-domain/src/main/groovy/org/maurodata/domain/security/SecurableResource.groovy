package org.maurodata.domain.security

import groovy.transform.CompileStatic
import io.micronaut.core.annotation.Introspected

@CompileStatic
@Introspected
trait SecurableResource {

    abstract UUID getId()

    abstract Boolean getReadableByEveryone()

    abstract Boolean getReadableByAuthenticatedUsers()
}