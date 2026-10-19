package org.maurodata.service.link

import groovy.transform.CompileStatic
import org.maurodata.domain.model.AdministeredItem
import org.maurodata.domain.model.Path

@CompileStatic
class ResolvedItemLinkPath {
    Path path
    AdministeredItem source
}
