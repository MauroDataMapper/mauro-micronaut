package org.maurodata.persistence.model.dto

import org.maurodata.domain.model.AdministeredItem

import groovy.transform.CompileStatic
import groovy.transform.SelfType

@CompileStatic
@SelfType(AdministeredItem)
trait AdministeredItemDTO implements Serializable {

    String getDomainType() {
        String domainType = this.class.simpleName

        domainType.endsWith('DTO') ? domainType[0..<-3] : domainType
    }
}