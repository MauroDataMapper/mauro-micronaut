package uk.ac.ox.softeng.mauro.profile

import jakarta.inject.Inject
import jakarta.inject.Singleton
import uk.ac.ox.softeng.mauro.domain.model.AdministeredItem
import uk.ac.ox.softeng.mauro.plugin.MauroPluginService

@Singleton
class ProfileService {

    @Inject
    MauroPluginService pluginService

    List<Profile> getStaticProfiles() {
        pluginService.listPlugins(Profile)
    }

    List<Profile> getUsedProfilesForAdministeredItem(List<Profile> profiles, AdministeredItem item) {
        profiles.findAll { profile ->
            item.metadata.namespace.contains(profile.namespace)
        }
    }

    List<Profile> getUnusedProfilesForAdministeredItem(List<Profile> profiles, AdministeredItem item) {
        profiles.findAll { profile ->
            !item.metadata.namespace.contains(profile.namespace)
        }
    }


}
