package org.maurodata

import groovy.util.logging.Slf4j
import jakarta.inject.Singleton
import org.maurodata.api.admin.AdminApi
import org.maurodata.api.classifier.ClassificationSchemeApi
import org.maurodata.api.classifier.ClassifierApi
import org.maurodata.api.config.ApiPropertyApi
import org.maurodata.api.config.SessionApi
import org.maurodata.api.dataflow.DataClassComponentApi
import org.maurodata.api.dataflow.DataElementComponentApi
import org.maurodata.api.dataflow.DataFlowApi
import org.maurodata.api.datamodel.DataClassApi
import org.maurodata.api.datamodel.DataElementApi
import org.maurodata.api.datamodel.DataModelApi
import org.maurodata.api.datamodel.DataTypeApi
import org.maurodata.api.datamodel.EnumerationValueApi
import org.maurodata.api.facet.AnnotationApi
import org.maurodata.api.facet.MetadataApi
import org.maurodata.api.facet.ReferenceFileApi
import org.maurodata.api.facet.SummaryMetadataApi
import org.maurodata.api.facet.SummaryMetadataReportApi
import org.maurodata.api.folder.FolderApi
import org.maurodata.api.importer.ImporterApi
import org.maurodata.api.profile.ProfileApi
import org.maurodata.api.search.SearchApi
import org.maurodata.api.security.CatalogueUserApi
import org.maurodata.api.security.SecurableResourceGroupRoleApi
import org.maurodata.api.security.UserGroupApi
import org.maurodata.api.security.openidprovider.OpenidProviderApi
import org.maurodata.api.terminology.CodeSetApi
import org.maurodata.api.terminology.TermApi
import org.maurodata.api.terminology.TermRelationshipApi
import org.maurodata.api.terminology.TermRelationshipTypeApi
import org.maurodata.api.terminology.TerminologyApi
import org.maurodata.api.tree.TreeApi
import org.maurodata.domain.datamodel.DataModel
import org.maurodata.domain.folder.Folder
import org.maurodata.domain.terminology.Terminology
import org.maurodata.export.ExportModel
import org.maurodata.plugin.exporter.json.JsonDataModelExporterPlugin
import org.maurodata.plugin.exporter.json.JsonFolderExporterPlugin
import org.maurodata.plugin.exporter.json.JsonTerminologyExporterPlugin
import org.maurodata.plugin.importer.json.JsonDataModelImporterPlugin
import org.maurodata.plugin.importer.json.JsonFolderImporterPlugin
import org.maurodata.plugin.importer.json.JsonTerminologyImporterPlugin
import org.maurodata.web.ListResponse

import com.fasterxml.jackson.databind.ObjectMapper
import io.micronaut.http.MediaType
import io.micronaut.http.client.multipart.MultipartBody
import jakarta.inject.Inject
import picocli.CommandLine.Command


@Singleton
@Command
@Slf4j
abstract class ApiClient {

    @Inject ObjectMapper objectMapper

    @Inject JsonTerminologyExporterPlugin jsonTerminologyExporterPlugin
    @Inject JsonTerminologyImporterPlugin jsonTerminologyImporterPlugin

    @Inject JsonDataModelExporterPlugin jsonDataModelExporterPlugin
    @Inject JsonDataModelImporterPlugin jsonDataModelImporterPlugin

    @Inject JsonFolderExporterPlugin jsonFolderExporterPlugin
    @Inject JsonFolderImporterPlugin jsonFolderImporterPlugin

    @Inject AdminApi adminApi
    @Inject ClassificationSchemeApi classificationSchemeApi
    @Inject ClassifierApi classifierApi
    @Inject ApiPropertyApi apiPropertyApi
    @Inject SessionApi sessionApi
    @Inject DataClassComponentApi dataClassComponentApi
    @Inject DataElementComponentApi dataElementComponentApi
    @Inject DataFlowApi dataFlowApi
    @Inject DataClassApi dataClassApi
    @Inject DataElementApi dataElementApi
    @Inject DataModelApi dataModelApi
    @Inject DataTypeApi dataTypeApi
    @Inject EnumerationValueApi enumerationValueApi
    @Inject AnnotationApi annotationApi
    @Inject MetadataApi metadataApi
    @Inject ReferenceFileApi referenceFileApi
    @Inject SummaryMetadataApi summaryMetadataApi
    @Inject SummaryMetadataReportApi summaryMetadataReportApi
    @Inject FolderApi folderApi
    @Inject ImporterApi importerApi
    @Inject ProfileApi profileApi
    @Inject SearchApi searchApi
    @Inject OpenidProviderApi openidProviderApi
    @Inject CatalogueUserApi catalogueUserApi
    @Inject SecurableResourceGroupRoleApi securableResourceGroupRoleApi
    @Inject UserGroupApi userGroupApi
    @Inject CodeSetApi codeSetApi
    @Inject TermApi termApi
    @Inject TerminologyApi terminologyApi
    @Inject TermRelationshipApi termRelationshipApi
    @Inject TermRelationshipTypeApi termRelationshipTypeApi
    @Inject TreeApi treeApi

    // Helper functions
    // TODO: Work out how to move these into the Api interfaces (traits?)
    Folder findOrCreateFolderByLabel(String label) {
        Folder folder = folderApi.listAll().find {it.label == label}
        if(!folder) {
            folder = folderApi.create(new Folder(label: label))
        }
        folder
    }


    ListResponse<Terminology> importTerminology(UUID folderId, Terminology terminology) {

        MultipartBody importRequest = MultipartBody.builder()
            .addPart('folderId', folderId.toString())
            .addPart('importFile', 'file.json', MediaType.APPLICATION_JSON_TYPE, jsonTerminologyExporterPlugin.exportModel(terminology))
            .build()

        terminologyApi.importModel(
            importRequest,
            jsonTerminologyImporterPlugin.namespace,
            jsonTerminologyImporterPlugin.name,
            jsonTerminologyImporterPlugin.version)
    }

    ListResponse<DataModel> importDataModel(UUID folderId, DataModel dataModel) {

        MultipartBody importRequest = MultipartBody.builder()
            .addPart('folderId', folderId.toString())
            .addPart('importFile', 'file.json', MediaType.APPLICATION_JSON_TYPE, jsonDataModelExporterPlugin.exportModel(dataModel))
            .build()

        dataModelApi.importModel(
            importRequest,
            jsonDataModelImporterPlugin.namespace,
            jsonDataModelImporterPlugin.name,
            jsonDataModelImporterPlugin.version)
    }

    ListResponse<Folder> importFolder(Folder folder, UUID parentFolderId = null) {

        MultipartBody.Builder importRequest = MultipartBody.builder()
            .addPart('importFile', 'file.json', MediaType.APPLICATION_JSON_TYPE, jsonFolderExporterPlugin.exportModel(folder))

        if(parentFolderId) {
            importRequest.addPart('folderId', parentFolderId.toString())
        }

        folderApi.importModel(
            importRequest.build(),
            jsonFolderImporterPlugin.namespace,
            jsonFolderImporterPlugin.name,
            jsonFolderImporterPlugin.version)
    }

    DataModel exportDataModel(UUID dataModelId) {
        def response = dataModelApi.exportModel(dataModelId, jsonDataModelExporterPlugin.namespace, jsonDataModelExporterPlugin.name, jsonDataModelExporterPlugin.version)
        ExportModel exportModel = objectMapper.readValue(response.body(), ExportModel)
        DataModel dataModel = exportModel.dataModel
        return dataModel
    }

    Terminology exportTerminology(UUID terminologyId) {
        def response = terminologyApi.exportModel(terminologyId, jsonTerminologyExporterPlugin.namespace, jsonTerminologyExporterPlugin.name, jsonTerminologyExporterPlugin.version)
        ExportModel exportModel = objectMapper.readValue(response.body(), ExportModel)
        Terminology terminology = exportModel.terminology
        return terminology
    }
}
