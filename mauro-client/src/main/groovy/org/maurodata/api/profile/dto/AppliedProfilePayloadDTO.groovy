package org.maurodata.api.profile.dto

import org.maurodata.profile.applied.AppliedProfile
import org.maurodata.profile.applied.AppliedProfileSection

import groovy.transform.CompileStatic
import io.micronaut.core.annotation.Introspected

@CompileStatic
@Introspected
class AppliedProfilePayloadDTO extends AppliedProfile implements Serializable {

    UUID id
    String label
    String domainType

    AppliedProfilePayloadDTO() {}

    AppliedProfilePayloadDTO(AppliedProfile appliedProfile) {
        id = appliedProfile.id
        label = appliedProfile.label
        domainType = appliedProfile.domainType
        name = appliedProfile.name
        version = appliedProfile.version
        displayName = appliedProfile.displayName
        description = appliedProfile.description
        namespace = appliedProfile.namespace
        providerType = appliedProfile.providerType
        metadataNamespace = appliedProfile.metadataNamespace
        profileApplicableForDomains = appliedProfile.profileApplicableForDomains
        sections = appliedProfile.sections
        errors = appliedProfile.errors
    }

    @Override
    UUID getId() {
        id ?: super.getId()
    }

    @Override
    String getLabel() {
        label ?: super.getLabel()
    }

    @Override
    String getDomainType() {
        domainType ?: super.getDomainType()
    }

    @Override
    List<AppliedProfileSection> getSections() {
        super.sections ?: []
    }
}
