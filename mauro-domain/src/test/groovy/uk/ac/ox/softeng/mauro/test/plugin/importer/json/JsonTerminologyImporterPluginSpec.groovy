package uk.ac.ox.softeng.mauro.test.plugin.importer.json

import uk.ac.ox.softeng.mauro.domain.datamodel.DataModel
import uk.ac.ox.softeng.mauro.domain.terminology.Terminology
import uk.ac.ox.softeng.mauro.export.ExportModel
import uk.ac.ox.softeng.mauro.plugin.MauroPluginService
import uk.ac.ox.softeng.mauro.plugin.importer.DataModelImporterPlugin
import uk.ac.ox.softeng.mauro.plugin.importer.FileImportParameters
import uk.ac.ox.softeng.mauro.plugin.importer.FileParameter
import uk.ac.ox.softeng.mauro.plugin.importer.TerminologyImporterPlugin
import uk.ac.ox.softeng.mauro.test.domain.datamodel.DataModelSpec
import uk.ac.ox.softeng.mauro.test.domain.terminology.TerminologySpec

import com.fasterxml.jackson.databind.ObjectMapper
import spock.lang.Specification

class JsonTerminologyImporterPluginSpec extends Specification  {

    static String NAMESPACE = "uk.ac.ox.softeng.mauro.plugin.importer.json"
    static String NAME = "JsonTerminologyImporterPlugin"
    static String VERSION = "4.0.0"

    ObjectMapper objectMapper = new ObjectMapper()

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

        TerminologyImporterPlugin jsonImportPlugin = MauroPluginService.getPlugin(TerminologyImporterPlugin, NAMESPACE, NAME, VERSION)
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
