package uk.ac.ox.softeng.mauro.profile

import uk.ac.ox.softeng.mauro.domain.facet.Metadata
import uk.ac.ox.softeng.mauro.domain.model.AdministeredItem
import uk.ac.ox.softeng.mauro.plugin.MauroPlugin
import uk.ac.ox.softeng.mauro.plugin.PluginType

trait Profile extends MauroPlugin {

    boolean canBeEditedAfterFinalisation
    List<String> profileApplicableForDomains

    List<ProfileSection> sections = []

    abstract String getMetadataNamespace()

    @Override
    PluginType getPluginType() {
        return PluginType.Profile
    }

    List<String> validate(AdministeredItem item) {
        List<String> errors = []
        if(!profileApplicableForDomains.contains(item.class.simpleName)) {
            errors.add("The profile '${displayName}' cannot be applied to an object of type '${item.class.simpleName}'.  Allowed types are $profileApplicableForDomains'")
        }
        List<Metadata> profileMetadata = item.metadata.findAll {it.namespace == metadataNamespace }
        sections.each { section ->
            errors.addAll(section.validate(item, profileMetadata))
        }
        return errors
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
        sections.collect { section ->
            section.fields.collect { field ->
                field.getMetadataKey(section.label)
            }
        }.flatten().sort()
    }

}
