package uk.ac.ox.softeng.mauro.domain.federation

import groovy.transform.CompileStatic
import io.micronaut.core.annotation.Introspected

@CompileStatic
@Introspected
class AuthorityResponse {
    String label
    String url

}
