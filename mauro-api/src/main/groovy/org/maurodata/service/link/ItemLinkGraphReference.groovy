package org.maurodata.service.link

import groovy.transform.CompileStatic
import org.maurodata.domain.model.Path

@CompileStatic
class ItemLinkGraphReference {
    UUID id
    UUID targetId
    String type
    Path sourcePath
}
