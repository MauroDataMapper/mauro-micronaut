package org.maurodata.test.plugin.importer.json

import org.maurodata.domain.terminology.Terminology
import org.maurodata.export.ExportModel
import org.maurodata.plugin.JsonPluginConstants
import org.maurodata.plugin.MauroPluginService
import org.maurodata.plugin.importer.FileImportParameters
import org.maurodata.plugin.importer.FileParameter
import org.maurodata.plugin.importer.TerminologyImporterPlugin
import org.maurodata.test.domain.terminology.TerminologySpec

import com.fasterxml.jackson.databind.ObjectMapper
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import jakarta.inject.Inject
import spock.lang.Specification

@MicronautTest
class JsonTerminologyImporterPluginSpec extends Specification  {

    static String NAMESPACE = JsonPluginConstants.NAMESPACE
    static String NAME = JsonPluginConstants.JSON_TERMINOLOGY_NAME
    static String VERSION = JsonPluginConstants.VERSION

    @Inject
    ObjectMapper objectMapper

    @Inject
    MauroPluginService mauroPluginService

    def "test JSON terminology import"() {

        when:
        Terminology testTerminology = TerminologySpec.testTerminology

        ExportModel exportModel = ExportModel.build {
            terminology testTerminology
            exportMetadata {
                namespace NAMESPACE
                name NAME
                version VERSION
            }
        }

        TerminologyImporterPlugin jsonImportPlugin = mauroPluginService.getPlugin(TerminologyImporterPlugin, NAMESPACE, NAME, VERSION)
        FileImportParameters fileImportParameters = new FileImportParameters()
        FileParameter fileParameter = new FileParameter()
        fileParameter.fileName = "import.json"
        fileParameter.fileType = "application/json"
        fileParameter.fileContents = objectMapper.writeValueAsBytes(exportModel)
        fileImportParameters.importFile = fileParameter

        List<Terminology> importedModels = jsonImportPlugin.importModels(fileImportParameters)

        then:
        importedModels.size() == 1
        // TODO: A diff between source and imported models would be good here
        importedModels.first().label == testTerminology.label
        importedModels.first().terms.size() == testTerminology.terms.size()
        importedModels.first().termRelationships.size() == testTerminology.termRelationships.size()
    }

}
