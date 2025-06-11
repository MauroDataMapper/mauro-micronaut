package org.maurodata.api.model

class VersionLinkDTO {
    UUID id
    String linkType
    final String domainType="VersionLink"
    ModelRefDTO sourceModel
    ModelRefDTO targetModel
}
