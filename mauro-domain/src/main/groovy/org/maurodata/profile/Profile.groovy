package org.maurodata.profile

import org.maurodata.domain.model.AdministeredItem
import org.maurodata.plugin.MauroPlugin
import org.maurodata.plugin.PluginType

import groovy.transform.CompileStatic

@CompileStatic
trait Profile extends MauroPlugin {

    boolean canBeEditedAfterFinalisation

    abstract List<String> getProfileApplicableForDomains()

    List<ProfileSection> sections = []

    abstract String getMetadataNamespace()

    @Override
    PluginType getPluginType() {
        return PluginType.Profile
    }



    boolean isApplicableForDomain(String domain) {
        return (
            this.getProfileApplicableForDomains() == null ||
            this.getProfileApplicableForDomains().size() == 0 ||
            this.getProfileApplicableForDomains().contains(domain))
    }

    boolean isApplicableForDomain(AdministeredItem item) {
        return isApplicableForDomain(item.getDomainType())
    }


    List<String> getKeys() {
        ((List<String>) sections.collect { section ->
            section.fields.collect { field ->
                field.getMetadataKey(section.label)
            }
        }.flatten()).sort()
    }

}
