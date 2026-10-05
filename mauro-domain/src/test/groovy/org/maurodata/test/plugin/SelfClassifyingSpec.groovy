package org.maurodata.test.plugin

import org.maurodata.plugin.MauroPlugin
import org.maurodata.plugin.MauroPluginService
import org.maurodata.plugin.PluginType
import org.maurodata.plugin.SelfClassifier
import org.maurodata.profile.ProfileSection

import spock.lang.Specification

class SelfClassifyingSpec extends Specification {

    void 'get classifying plugins filters by classifier namespace and label'() {
        given:

        MauroPlugin databaseProfile = testPlugin(
            PluginType.Profile,
            'org.maurodata.database',
            [
                new SelfClassifier(namespace: 'org.maurodata.database', label: 'Table', description: 'This is a database table in a schema'),
                new SelfClassifier(namespace: 'org.maurodata.database', label: 'Column', description: 'This is a database column')
            ])

        MauroPlugin formSectionProfile = testPlugin(
            PluginType.Profile,
            'org.maurodata.form',
            [
                new SelfClassifier(namespace: 'org.maurodata.form', label: 'Section', description: 'This is a section of a form, and may hold questions')
            ])

        MauroPlugin dictionaryAttributeProfile = testPlugin(
            PluginType.Profile,
            'org.maurodata.dictionary',
            [
                new SelfClassifier(namespace: 'org.maurodata.dictionary', label: 'Abstract attribute', description: 'This is an abstract attribute of a Data Dictionary')
            ])

        MauroPlugin UMLProfile = testPlugin(
            PluginType.Profile,
            'org.omg.uml',
            [
                new SelfClassifier(namespace: 'org.omg.uml', label: 'Class Diagram', description: 'This is a Class Diagram')
            ])

        MauroPlugin databaseImporter = testPlugin(
            PluginType.Importer,
            'org.maurodata.database',
            [
                new SelfClassifier(namespace: 'org.maurodata.database', label: 'Database', description: 'This profiles and catalogues a database'),
            ])

        MauroPlugin svgExport = testPlugin(
            PluginType.Exporter,
            'org.w3.xml',
            [
                new SelfClassifier(namespace: 'http://www.w3.org/2000/svg', label: 'SVG', description: 'An SVG export'),
            ])

        MauroPlugin unclassifiedPlugin= testPlugin(PluginType.Importer, 'unclassifiedProfile', [])

        MauroPluginService pluginService = new TestMauroPluginService(
            plugins: [databaseProfile, formSectionProfile, dictionaryAttributeProfile, UMLProfile, databaseImporter, svgExport, unclassifiedPlugin])

        expect:
        pluginService.getClassifyingPlugins(null) == [databaseProfile, formSectionProfile, dictionaryAttributeProfile, UMLProfile, databaseImporter, svgExport]
        pluginService.getClassifyingPlugins('Profile') == [databaseProfile, formSectionProfile, dictionaryAttributeProfile, UMLProfile]
        pluginService.getClassifyingPlugins('Profile', 'org.maurodata.database') == [databaseProfile]
        pluginService.getClassifyingPlugins('Profile','org.maurodata.database', 'Column') == [databaseProfile]
        pluginService.getClassifyingPlugins('Importer', 'org.maurodata.database', 'Missing').isEmpty()
        pluginService.getClassifyingPlugins(null, 'missing').isEmpty()
    }

    private static MauroPlugin testPlugin(PluginType pluginType, String metadataNamespace, List<SelfClassifier> classifiers) {
        new TestPlugin(
            pluginType: pluginType,
            metadataNamespace: metadataNamespace,
            profileApplicableForDomains: [],
            classifiers: classifiers)
    }

    static class TestPlugin implements MauroPlugin {
        String metadataNamespace
        List<String> profileApplicableForDomains
        List<SelfClassifier> classifiers
        List<ProfileSection> sections = []
        boolean canBeEditedAfterFinalisation = false
        String version = '1.0.0'
        String displayName = 'Test Profile'
        PluginType pluginType = PluginType.Profile
    }

    static class TestMauroPluginService extends MauroPluginService {
        List<MauroPlugin> plugins

        @Override
        List<MauroPlugin> listPlugins() {
            plugins
        }
    }
}
