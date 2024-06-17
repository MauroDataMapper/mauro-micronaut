package uk.ac.ox.softeng.mauro.profile

import uk.ac.ox.softeng.mauro.plugin.MauroPlugin
import uk.ac.ox.softeng.mauro.plugin.PluginType

trait Profile extends MauroPlugin {

    List<String> applicableForDomains = []

    List<ProfileSection> sections = []


    @Override
    PluginType getPluginType() {
        return PluginType.Profile
    }


}
