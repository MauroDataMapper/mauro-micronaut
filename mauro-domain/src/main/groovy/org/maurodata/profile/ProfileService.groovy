package org.maurodata.profile

import groovy.transform.CompileStatic
import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.maurodata.domain.model.AdministeredItem
import org.maurodata.plugin.MauroPluginService

@CompileStatic
@Singleton
class ProfileService {

    @Inject
    MauroPluginService pluginService

    List<Profile> getStaticProfiles() {
        pluginService.listPlugins(Profile)
    }

    List<Profile> getClassifyingProfiles() {
        getStaticProfiles().findAll {Profile profile ->
            profile.getClassifiers()
        }
    }

    List<Profile> getClassifyingProfiles(String classifierNamespace) {
        getClassifyingProfiles().findAll {Profile profile ->
            profile.getClassifiers().find {ProfileClassifier classifier ->
                classifier.namespace == classifierNamespace
            }
        }
    }

    List<Profile> getClassifyingProfiles(String classifierNamespace, String classifierLabel) {
        getClassifyingProfiles().findAll {Profile profile ->
            profile.getClassifiers().find {ProfileClassifier classifier ->
                classifier.namespace == classifierNamespace &&
                    classifier.label == classifierLabel
            }
        }
    }

    List<Profile> getUsedProfilesForAdministeredItem(List<Profile> profiles, AdministeredItem item) {
        profiles.findAll { profile ->
            profile.isApplicableForDomain(item) &&
            item.getMetadata().find{ it.namespace == profile.metadataNamespace }
        }
    }

    List<Profile> getUnusedProfilesForAdministeredItem(List<Profile> profiles, AdministeredItem item) {
        profiles.findAll { profile ->
            profile.isApplicableForDomain(item) &&
            !item.getMetadata().find{ it.namespace == profile.metadataNamespace }
        }
    }

}
