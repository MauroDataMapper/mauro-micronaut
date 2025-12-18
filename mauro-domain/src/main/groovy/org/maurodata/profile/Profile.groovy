package org.maurodata.profile

import org.maurodata.domain.model.AdministeredItem
import org.maurodata.plugin.MauroPlugin
import org.maurodata.plugin.PluginType

import groovy.transform.CompileStatic

@CompileStatic
trait Profile extends MauroPlugin {

    boolean canBeEditedAfterFinalisation
    List<String> profileApplicableForDomains

    List<ProfileSection> sections = []

    abstract String getMetadataNamespace()

    @Override
    PluginType getPluginType() {
        return PluginType.Profile
    }



    boolean isApplicableForDomain(String domain) {
        (profileApplicableForDomains == null ||
                profileApplicableForDomains.size() == 0 ||
                profileApplicableForDomains.contains(domain))
    }

    boolean isApplicableForDomain(AdministeredItem item) {
        (profileApplicableForDomains == null ||
                profileApplicableForDomains.size() == 0 ||
                profileApplicableForDomains.contains(item.getDomainType()))
    }


    List<String> getKeys() {
        ((List<String>) sections.collect { section ->
            section.fields.collect { field ->
                field.getMetadataKey(section.label)
            }
        }.flatten()).sort()
    }

}
