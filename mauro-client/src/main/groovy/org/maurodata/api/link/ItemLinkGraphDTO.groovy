package org.maurodata.api.link

import groovy.transform.CompileStatic
import io.micronaut.core.annotation.Introspected

@Introspected
@CompileStatic
class ItemLinkGraphDTO {
    List<ItemLinkItemDTO> items = []
    List<ItemLinkDTO> links = []
}
