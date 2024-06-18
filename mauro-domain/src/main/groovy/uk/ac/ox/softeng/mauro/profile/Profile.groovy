package uk.ac.ox.softeng.mauro.profile

import uk.ac.ox.softeng.mauro.domain.facet.Metadata
import uk.ac.ox.softeng.mauro.domain.model.AdministeredItem
import uk.ac.ox.softeng.mauro.plugin.MauroPlugin
import uk.ac.ox.softeng.mauro.plugin.PluginType

trait Profile extends MauroPlugin {

    String metadataNamespace
    boolean canBeEditedAfterFinalisation
    List<String> profileApplicableForDomains


    List<String> applicableForDomains = []

    List<ProfileSection> sections = []


    @Override
    PluginType getPluginType() {
        return PluginType.Profile
    }

    List<String> validate(AdministeredItem item) {
        List<String> errors = []
        if(!applicableForDomains.contains(item.class.name)) {
            errors.add("The profile '${displayName}' cannot be applied to an object of type '${item.class.name}'")
        }
        List<Metadata> profileMetadata = item.metadata.findAll {it.namespace == metadataNamespace }
        sections.each { section ->
            errors.addAll(section.validate(item, profileMetadata))
        }
        return errors

    }

}
