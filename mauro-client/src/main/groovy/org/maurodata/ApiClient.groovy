package org.maurodata

import uk.ac.ox.softeng.mauro.api.admin.AdminApi
import uk.ac.ox.softeng.mauro.api.classifier.ClassificationSchemeApi
import uk.ac.ox.softeng.mauro.api.classifier.ClassifierApi
import uk.ac.ox.softeng.mauro.api.config.ApiPropertyApi
import uk.ac.ox.softeng.mauro.api.config.SessionApi
import uk.ac.ox.softeng.mauro.api.dataflow.DataClassComponentApi
import uk.ac.ox.softeng.mauro.api.dataflow.DataElementComponentApi
import uk.ac.ox.softeng.mauro.api.dataflow.DataFlowApi
import uk.ac.ox.softeng.mauro.api.datamodel.DataClassApi
import uk.ac.ox.softeng.mauro.api.datamodel.DataElementApi
import uk.ac.ox.softeng.mauro.api.datamodel.DataModelApi
import uk.ac.ox.softeng.mauro.api.datamodel.DataTypeApi
import uk.ac.ox.softeng.mauro.api.datamodel.EnumerationValueApi
import uk.ac.ox.softeng.mauro.api.facet.AnnotationApi
import uk.ac.ox.softeng.mauro.api.facet.MetadataApi
import uk.ac.ox.softeng.mauro.api.facet.ReferenceFileApi
import uk.ac.ox.softeng.mauro.api.facet.SummaryMetadataApi
import uk.ac.ox.softeng.mauro.api.facet.SummaryMetadataReportApi
import uk.ac.ox.softeng.mauro.api.folder.FolderApi
import uk.ac.ox.softeng.mauro.api.importer.ImporterApi
import uk.ac.ox.softeng.mauro.api.profile.ProfileApi
import uk.ac.ox.softeng.mauro.api.search.SearchApi
import uk.ac.ox.softeng.mauro.api.security.CatalogueUserApi
import uk.ac.ox.softeng.mauro.api.security.SecurableResourceGroupRoleApi
import uk.ac.ox.softeng.mauro.api.security.UserGroupApi
import uk.ac.ox.softeng.mauro.api.security.openidprovider.OpenidProviderApi
import uk.ac.ox.softeng.mauro.api.terminology.CodeSetApi
import uk.ac.ox.softeng.mauro.api.terminology.TermApi
import uk.ac.ox.softeng.mauro.api.terminology.TermRelationshipApi
import uk.ac.ox.softeng.mauro.api.terminology.TermRelationshipTypeApi
import uk.ac.ox.softeng.mauro.api.terminology.TerminologyApi
import uk.ac.ox.softeng.mauro.api.tree.TreeApi
import uk.ac.ox.softeng.mauro.domain.folder.Folder
import uk.ac.ox.softeng.mauro.domain.terminology.Terminology
import uk.ac.ox.softeng.mauro.plugin.exporter.json.JsonTerminologyExporterPlugin
import uk.ac.ox.softeng.mauro.web.ListResponse

import com.fasterxml.jackson.databind.ObjectMapper
import io.micronaut.configuration.picocli.PicocliRunner
import io.micronaut.http.MediaType
import io.micronaut.http.client.multipart.MultipartBody
import jakarta.inject.Inject
import org.slf4j.Logger
import picocli.CommandLine.Command

import static org.slf4j.LoggerFactory.getLogger


@Command(name="my-cli-app")
abstract class ApiClient implements Runnable {

    protected static final Logger log = getLogger(ApiClient)

    @Inject ObjectMapper objectMapper

    @Inject JsonTerminologyExporterPlugin jsonTerminologyExporterPlugin

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


    static void main(String[] args) {
        PicocliRunner.run(ApiClient, args)
    }

    abstract void run()


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
            'uk.ac.ox.softeng.mauro.plugin.importer.json',
            'JsonTerminologyImporterPlugin',
            '4.0.0')

    }

}
