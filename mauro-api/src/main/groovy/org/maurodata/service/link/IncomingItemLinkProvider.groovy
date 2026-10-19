package org.maurodata.service.link

import groovy.transform.CompileStatic
import org.maurodata.domain.model.AdministeredItem

@CompileStatic
interface IncomingItemLinkProvider {
    String getType()
    String getLabel()
    List<IncomingItemLink> findIncomingLinks(AdministeredItem target)
}
