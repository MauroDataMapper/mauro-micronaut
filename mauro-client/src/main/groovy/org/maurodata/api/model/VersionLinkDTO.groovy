package org.maurodata.api.model

import groovy.transform.CompileStatic

@CompileStatic
class VersionLinkDTO {
    UUID id
    String linkType
    final String domainType="VersionLink"
    ModelRefDTO sourceModel
    ModelRefDTO targetModel
}
