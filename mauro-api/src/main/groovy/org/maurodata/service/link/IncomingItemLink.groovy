package org.maurodata.service.link

import groovy.transform.CompileStatic
import org.maurodata.domain.model.Path

@CompileStatic
class IncomingItemLink {
    UUID id
    String type
    Path path
}
