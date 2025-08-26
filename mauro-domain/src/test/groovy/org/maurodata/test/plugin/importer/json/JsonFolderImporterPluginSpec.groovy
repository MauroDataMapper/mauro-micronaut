package org.maurodata.test.plugin.importer.json

import com.fasterxml.jackson.databind.ObjectMapper
import io.micronaut.http.HttpStatus
import io.micronaut.http.MediaType
import io.micronaut.http.exceptions.HttpStatusException
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import jakarta.inject.Inject
import org.maurodata.domain.datamodel.DataModel
import org.maurodata.domain.folder.Folder
import org.maurodata.domain.terminology.Terminology
import org.maurodata.export.ExportModel
import org.maurodata.plugin.JsonPluginConstants
import org.maurodata.plugin.MauroPluginService
import org.maurodata.plugin.importer.FileImportParameters
import org.maurodata.plugin.importer.FileParameter
import org.maurodata.plugin.importer.FolderImporterPlugin
import org.maurodata.test.domain.datamodel.DataModelSpec
import org.maurodata.test.domain.folder.FolderSpec
import org.maurodata.test.domain.terminology.TerminologySpec
import spock.lang.Specification

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
        diffResult.diffs.isEmpty()

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
        diffResult.diffs.isEmpty()

        importedModels.first().label == testFolder.label
        importedModels.first().description == testFolder.description
    }


    def "test JSON folders import- bad file -should fail with BADREQUEST exception"() {
        given:
        ExportModel exportModel = ExportModel.build {
            exportMetadata {
                namespace JsonPluginConstants.NAMESPACE
                name JsonPluginConstants.JSON_FOLDER_NAME
                version JsonPluginConstants.VERSION
            }
        }
        FolderImporterPlugin jsonImportPlugin = mauroPluginService.getPlugin(FolderImporterPlugin, JsonPluginConstants.NAMESPACE, JsonPluginConstants.JSON_FOLDER_NAME,
                                                                             JsonPluginConstants.VERSION)

        FileImportParameters fileImportParameters = new FileImportParameters()
        FileParameter fileParameter = new FileParameter().tap {
            fileName = "import.json"
            fileType = MediaType.APPLICATION_JSON
            fileContents = objectMapper.writeValueAsBytes(exportModel)
        }
        fileImportParameters.importFile = fileParameter

        when:
        jsonImportPlugin.importModels(fileImportParameters)

        then:
        HttpStatusException exception = thrown()
        exception.status == HttpStatus.BAD_REQUEST
    }
}

