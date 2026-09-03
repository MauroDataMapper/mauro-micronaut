package org.maurodata.test.profile

import org.maurodata.plugin.MauroPluginService
import org.maurodata.profile.Profile
import org.maurodata.profile.ProfileClassifier
import org.maurodata.profile.ProfileSection
import org.maurodata.profile.ProfileService

import spock.lang.Specification

class ProfileServiceSpec extends Specification {

    void 'get classifying profiles filters by classifier namespace and label'() {
        given:
        Profile semanticProfile = testProfile(
            'semanticProfile',
            [
                new ProfileClassifier(namespace: 'semantic', label: 'Entity', description: 'Semantic entity'),
                new ProfileClassifier(namespace: 'semantic', label: 'Attribute', description: 'Semantic attribute')
            ])
        Profile structuralProfile = testProfile(
            'structuralProfile',
            [
                new ProfileClassifier(namespace: 'structural', label: 'Table', description: 'Structural table')
            ])
        Profile unclassifiedProfile = testProfile('unclassifiedProfile', [])

        ProfileService profileService = new ProfileService(
            pluginService: new TestMauroPluginService(
                profiles: [semanticProfile, structuralProfile, unclassifiedProfile]))

        expect:
        profileService.getClassifyingProfiles() == [semanticProfile, structuralProfile]
        profileService.getClassifyingProfiles('semantic') == [semanticProfile]
        profileService.getClassifyingProfiles('semantic', 'Entity') == [semanticProfile]
        profileService.getClassifyingProfiles('semantic', 'Missing').isEmpty()
        profileService.getClassifyingProfiles('missing').isEmpty()
    }

    private static Profile testProfile(String metadataNamespace, List<ProfileClassifier> classifiers) {
        new TestProfile(
            metadataNamespace: metadataNamespace,
            profileApplicableForDomains: [],
            classifiers: classifiers)
    }

    static class TestProfile implements Profile {
        String metadataNamespace
        List<String> profileApplicableForDomains
        List<ProfileClassifier> classifiers
        List<ProfileSection> sections = []
        boolean canBeEditedAfterFinalisation = false
        String version = '1.0.0'
        String displayName = 'Test Profile'
    }

    static class TestMauroPluginService extends MauroPluginService {
        List<Profile> profiles

        @Override
        <P> List<P> listPlugins(Class<P> pluginType) {
            profiles.findAll {
                pluginType.isInstance(it)
            } as List<P>
        }
    }
}
