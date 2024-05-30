package uk.ac.ox.softeng.mauro.test.plugin.importer.json

import com.fasterxml.jackson.databind.ObjectMapper
import io.micronaut.http.MediaType
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import jakarta.inject.Inject
import spock.lang.Specification
import uk.ac.ox.softeng.mauro.domain.datamodel.DataModel
import uk.ac.ox.softeng.mauro.domain.folder.Folder
import uk.ac.ox.softeng.mauro.domain.terminology.Terminology
import uk.ac.ox.softeng.mauro.export.ExportModel
import uk.ac.ox.softeng.mauro.plugin.JsonPluginConstants
import uk.ac.ox.softeng.mauro.plugin.MauroPluginService
import uk.ac.ox.softeng.mauro.plugin.importer.FileImportParameters
import uk.ac.ox.softeng.mauro.plugin.importer.FileParameter
import uk.ac.ox.softeng.mauro.plugin.importer.FolderImporterPlugin
import uk.ac.ox.softeng.mauro.test.domain.datamodel.DataModelSpec
import uk.ac.ox.softeng.mauro.test.domain.folder.FolderSpec
import uk.ac.ox.softeng.mauro.test.domain.terminology.TerminologySpec

@MicronautTest
class JsonFolderImporterPluginSpec extends Specification {

    @Inject
    ObjectMapper objectMapper

    @Inject
    MauroPluginService mauroPluginService


    def "test simple JSON folder import"() {
        when:
        Folder testFolder = FolderSpec.testFolder

        ExportModel exportModel = ExportModel.build {
            folder testFolder
            exportMetadata {
                namespace JsonPluginConstants.NAMESPACE
                name JsonPluginConstants.JSON_FOLDER_NAME
                version JsonPluginConstants.VERSION
            }
        }

        FolderImporterPlugin jsonImportPlugin = mauroPluginService.getPlugin(FolderImporterPlugin,JsonPluginConstants.NAMESPACE,
                JsonPluginConstants.JSON_FOLDER_NAME, JsonPluginConstants.VERSION)
        FileImportParameters fileImportParameters = new FileImportParameters()
        FileParameter fileParameter = new FileParameter()
        fileParameter.fileName = "import.json"
        fileParameter.fileType = MediaType.APPLICATION_JSON
        fileParameter.fileContents = objectMapper.writeValueAsBytes(exportModel)
        fileImportParameters.importFile = fileParameter

        List<Folder> importedModels = jsonImportPlugin.importModels(fileImportParameters)

        then:
        importedModels.size() == 1
        importedModels[0].label == FolderSpec.LABEL
        importedModels[0].author == FolderSpec.AUTHOR
        importedModels[0].description == FolderSpec.DESCRIPTION

        def diffResult = importedModels.first().diff(testFolder)
        diffResult.diffs.size() == 1
        diffResult.diffs[0].getName() == "createdBy"

        importedModels.first().label == testFolder.label
        importedModels.first().description == testFolder.description
    }

    def "test JSON folder, datamodel import"() {
        when:
        Folder testFolder = FolderSpec.testFolder
        DataModel testDataModel = DataModelSpec.testDataModel
        Terminology testTerminology = TerminologySpec.testTerminology

        ExportModel exportModel = ExportModel.build {
            folder testFolder
            dataModel testDataModel
            terminology testTerminology
            exportMetadata {
                namespace JsonPluginConstants.NAMESPACE
                name JsonPluginConstants.JSON_FOLDER_NAME
                version JsonPluginConstants.VERSION
            }
        }

        FolderImporterPlugin jsonImportPlugin = mauroPluginService.getPlugin(FolderImporterPlugin,JsonPluginConstants.NAMESPACE,
                JsonPluginConstants.JSON_FOLDER_NAME, JsonPluginConstants.VERSION)
        FileImportParameters fileImportParameters = new FileImportParameters()
        FileParameter fileParameter = new FileParameter()
        fileParameter.fileName = "import.json"
        fileParameter.fileType = MediaType.APPLICATION_JSON
        fileParameter.fileContents = objectMapper.writeValueAsBytes(exportModel)
        fileImportParameters.importFile = fileParameter

        List<Folder> importedModels = jsonImportPlugin.importModels(fileImportParameters)

        then:
        importedModels.size() == 1
        importedModels[0].label == FolderSpec.LABEL
        importedModels[0].author == FolderSpec.AUTHOR
        importedModels[0].description == FolderSpec.DESCRIPTION

        def diffResult = importedModels.first().diff(testFolder)
        diffResult.diffs.size() == 1
        diffResult.diffs[0].getName() == "createdBy"

        importedModels.first().label == testFolder.label
        importedModels.first().description == testFolder.description
    }
}

