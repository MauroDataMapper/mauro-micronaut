package uk.ac.ox.softeng.mauro.domain.federation

import groovy.transform.CompileStatic
import io.micronaut.core.annotation.Introspected

@CompileStatic
@Introspected
class MauroLink {
    String url
    String contentType

    MauroLink(String url, String contentType) {
        this.url = url
        this.contentType = contentType
    }
}
