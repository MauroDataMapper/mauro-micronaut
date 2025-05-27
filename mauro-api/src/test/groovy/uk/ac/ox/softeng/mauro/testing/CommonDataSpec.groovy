package uk.ac.ox.softeng.mauro.testing

import uk.ac.ox.softeng.mauro.api.SessionHandlerClientFilter
import uk.ac.ox.softeng.mauro.api.admin.AdminApi
import uk.ac.ox.softeng.mauro.api.authority.AuthorityApi
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
import uk.ac.ox.softeng.mauro.api.facet.EditApi
import uk.ac.ox.softeng.mauro.api.facet.ReferenceFileApi
import uk.ac.ox.softeng.mauro.api.facet.RuleApi
import uk.ac.ox.softeng.mauro.api.facet.RuleRepresentationApi
import uk.ac.ox.softeng.mauro.api.facet.SummaryMetadataApi
import uk.ac.ox.softeng.mauro.api.facet.SummaryMetadataReportApi
import uk.ac.ox.softeng.mauro.api.federation.PublishApi
import uk.ac.ox.softeng.mauro.api.federation.SubscribedCatalogueApi
import uk.ac.ox.softeng.mauro.api.federation.SubscribedModelApi
import uk.ac.ox.softeng.mauro.api.folder.FolderApi
import uk.ac.ox.softeng.mauro.api.importer.ImporterApi
import uk.ac.ox.softeng.mauro.api.profile.ProfileApi
import uk.ac.ox.softeng.mauro.api.search.SearchApi
import uk.ac.ox.softeng.mauro.api.security.ApiKeyApi
import uk.ac.ox.softeng.mauro.api.security.CatalogueUserApi
import uk.ac.ox.softeng.mauro.api.security.LoginApi
import uk.ac.ox.softeng.mauro.api.security.SecurableResourceGroupRoleApi
import uk.ac.ox.softeng.mauro.api.security.UserGroupApi
import uk.ac.ox.softeng.mauro.api.security.openidprovider.OpenidProviderApi
import uk.ac.ox.softeng.mauro.api.terminology.CodeSetApi
import uk.ac.ox.softeng.mauro.api.terminology.TermApi
import uk.ac.ox.softeng.mauro.api.terminology.TermRelationshipApi
import uk.ac.ox.softeng.mauro.api.terminology.TermRelationshipTypeApi
import uk.ac.ox.softeng.mauro.api.terminology.TerminologyApi
import uk.ac.ox.softeng.mauro.api.tree.TreeApi
import uk.ac.ox.softeng.mauro.domain.authority.Authority
import uk.ac.ox.softeng.mauro.domain.classifier.ClassificationScheme
import uk.ac.ox.softeng.mauro.domain.classifier.Classifier
import uk.ac.ox.softeng.mauro.domain.dataflow.DataFlow
import uk.ac.ox.softeng.mauro.domain.datamodel.DataClass
import uk.ac.ox.softeng.mauro.domain.datamodel.DataElement
import uk.ac.ox.softeng.mauro.domain.datamodel.DataModel
import uk.ac.ox.softeng.mauro.domain.datamodel.DataType
import uk.ac.ox.softeng.mauro.domain.facet.Annotation
import uk.ac.ox.softeng.mauro.domain.facet.Metadata
import uk.ac.ox.softeng.mauro.domain.facet.ReferenceFile
import uk.ac.ox.softeng.mauro.domain.facet.Rule
import uk.ac.ox.softeng.mauro.domain.facet.RuleRepresentation
import uk.ac.ox.softeng.mauro.domain.facet.SummaryMetadata
import uk.ac.ox.softeng.mauro.domain.facet.SummaryMetadataType
import uk.ac.ox.softeng.mauro.domain.facet.federation.SubscribedCatalogue
import uk.ac.ox.softeng.mauro.domain.facet.federation.SubscribedCatalogueAuthenticationType
import uk.ac.ox.softeng.mauro.domain.facet.federation.SubscribedCatalogueType
import uk.ac.ox.softeng.mauro.domain.facet.federation.SubscribedModel
import uk.ac.ox.softeng.mauro.domain.facet.federation.SubscribedModelFederationParams
import uk.ac.ox.softeng.mauro.domain.folder.Folder
import uk.ac.ox.softeng.mauro.domain.facet.SummaryMetadataReport
import uk.ac.ox.softeng.mauro.domain.model.version.FinaliseData
import uk.ac.ox.softeng.mauro.domain.model.version.VersionChangeType
import uk.ac.ox.softeng.mauro.domain.terminology.CodeSet
import uk.ac.ox.softeng.mauro.domain.terminology.Term
import uk.ac.ox.softeng.mauro.domain.terminology.TermRelationshipType
import uk.ac.ox.softeng.mauro.domain.terminology.Terminology
import uk.ac.ox.softeng.mauro.export.ExportModel
import uk.ac.ox.softeng.mauro.importdata.ImportMetadata
import uk.ac.ox.softeng.mauro.plugin.importer.json.JsonDataModelImporterPlugin
import uk.ac.ox.softeng.mauro.plugin.importer.json.JsonTerminologyImporterPlugin
import uk.ac.ox.softeng.mauro.web.ListResponse

import com.fasterxml.jackson.databind.ObjectMapper
import io.micronaut.http.MediaType
import io.micronaut.http.client.multipart.MultipartBody
import jakarta.inject.Inject
import spock.lang.Shared
import spock.lang.Specification

import java.time.Instant
import java.time.temporal.ChronoUnit


class CommonDataSpec extends Specification {

    public static final String AUTHOR = 'author'
    public static final String PATH_IDENTIFIER = 'pathIdentifier'
    public static final String PATH_MODEL_IDENTIFIER = 'pathModelIdentifier'
    public static final String MODEL_VERSION_TAG = 'modelVersionTag'
    public static final String FINALISED = 'finalised'
    public static final String DATE_FINALISED = 'dateFinalised'

    // Round up to deal with rounding errors in testing
    public static final Instant REPORT_DATE = Instant.now().truncatedTo(ChronoUnit.SECONDS)

    @Inject
    JsonDataModelImporterPlugin jsonDataModelImporterPlugin

    @Inject
    JsonTerminologyImporterPlugin jsonTerminologyImporterPlugin

    @Inject
    ObjectMapper objectMapper

    @Shared @Inject AdminApi adminApi
    @Shared @Inject ClassificationSchemeApi classificationSchemeApi
    @Shared @Inject ClassifierApi classifierApi
    @Shared @Inject ApiPropertyApi apiPropertyApi
    @Shared @Inject SessionApi sessionApi
    @Shared @Inject DataClassComponentApi dataClassComponentApi
    @Shared @Inject DataElementComponentApi dataElementComponentApi
    @Shared @Inject DataFlowApi dataFlowApi
    @Shared @Inject DataClassApi dataClassApi
    @Shared @Inject DataElementApi dataElementApi
    @Shared @Inject DataModelApi dataModelApi
    @Shared @Inject DataTypeApi dataTypeApi
    @Shared @Inject EnumerationValueApi enumerationValueApi
    @Shared @Inject AnnotationApi annotationApi
    @Shared @Inject MetadataApi metadataApi
    @Shared @Inject EditApi editApi
    @Shared @Inject ReferenceFileApi referenceFileApi
    @Shared @Inject SummaryMetadataApi summaryMetadataApi
    @Shared @Inject SummaryMetadataReportApi summaryMetadataReportApi
    @Shared @Inject RuleApi ruleApi
    @Shared @Inject RuleRepresentationApi ruleRepresentationApi
    @Shared @Inject FolderApi folderApi
    @Shared @Inject ImporterApi importerApi
    @Shared @Inject ProfileApi profileApi
    @Shared @Inject SearchApi searchApi
    @Shared @Inject OpenidProviderApi openidProviderApi
    @Shared @Inject CatalogueUserApi catalogueUserApi
    @Shared @Inject SecurableResourceGroupRoleApi securableResourceGroupRoleApi
    @Shared @Inject UserGroupApi userGroupApi
    @Shared @Inject CodeSetApi codeSetApi
    @Shared @Inject TermApi termApi
    @Shared @Inject TerminologyApi terminologyApi
    @Shared @Inject TermRelationshipApi termRelationshipApi
    @Shared @Inject TermRelationshipTypeApi termRelationshipTypeApi
    @Shared @Inject TreeApi treeApi
    @Shared @Inject LoginApi loginApi
    @Shared @Inject ApiKeyApi apiKeyApi
    @Shared @Inject AuthorityApi authorityApi
    @Shared @Inject SubscribedCatalogueApi subscribedCatalogueApi
    @Shared @Inject SubscribedModelApi subscribedModelApi
    @Shared @Inject PublishApi publishApi

    @Inject
    SessionHandlerClientFilter sessionHandlerClientFilter

    @Shared @Inject LowLevelApi lowLevelApi


    CodeSet codeSet() {
        new CodeSet(
                label       : "Test code set",
                description : "code set description",
                author      : "A.N. Other",
                organisation: "uk.ac.gridpp.ral.org"
        )
    }

    Folder folder() {
        Folder.build {
            label 'Test folder'
        }
    }


    Terminology terminology() {
        new Terminology(label: 'Test terminology')
    }

    Term termPayload() {
        new Term(code: 'B15.0', definition: 'Hepatitis A with hepatic coma')
    }

    SummaryMetadataReport summaryMetadataReport() {
        new SummaryMetadataReport(reportValue: 'test-report-value', reportDate: REPORT_DATE)
    }

    RuleRepresentation ruleRepresentation() {
        new RuleRepresentation(language: 'java', representation: 'age >= 0')
    }

    Annotation annotationPayload() {
        new Annotation(label: 'test-label', description: 'test-annotation description')
    }

    Annotation annotationPayload(String label, String description) {
        new Annotation(label: label, description: description)
    }

    SummaryMetadata summaryMetadataPayload() {
        new SummaryMetadata( summaryMetadataType: SummaryMetadataType.STRING,
                             label: 'summary metadata label')
    }

    Rule rulePayload() {
        new Rule(description: 'My first rule description', name: 'rule name')
    }

    Metadata metadataPayload() {
        Metadata.build {
            namespace 'org.example'
            key 'example_key'
            value 'example_value'
        }
    }

    DataModel dataModelPayload(){
        dataModelPayload("test label ")

    }

    DataModel dataModelPayload(String label){
        new DataModel(label: label, description: 'test description', author: 'test author')
    }

    DataClass dataClassPayload() {
        dataClassPayload('Test data class')
    }

    DataClass dataClassPayload(String label){
        new DataClass(label: label, description: 'test description')
    }

    DataElement dataElementPayload(String label, DataType dataType){
        new DataElement(
            label: label,
            description: 'test description',
            author: 'test author',
            dataType: dataType)
    }

    DataType dataTypesPayload(){
        new DataType(
            label: 'Test data type',
            dataTypeKind: DataType.DataTypeKind.PRIMITIVE_TYPE,
            units : 'kilograms')
    }

    TermRelationshipType termRelationshipType(){
       new TermRelationshipType(label: 'Test Term Relationship Type label',
                                //displayLabel: 'Random display label',
                                parentalRelationship: false,
                                childRelationship : false)
    }

    Term term(){
        new Term(description : 'Test Term description',
                 code: 'est',
                 definition: 'doloreum-et-val',
                 url : 'https://www.hello.com/test')
    }

    DataFlow dataFlowPayload(UUID sourceId){
        new DataFlow(
            label: 'test label',
            description: 'dataflow payload description ',
            source: new DataModel(id: sourceId))
    }

    ReferenceFile referenceFilePayload(){
        referenceFilePayload('reference file name')
    }
    ReferenceFile referenceFilePayload(String fileName){
        referenceFilePayload(fileName, 'this is a string file contents')
    }
    ReferenceFile referenceFilePayload(String fileName,String content){
        new ReferenceFile(
                fileName: fileName,
                fileSize: content.size(),
                fileContents: content.bytes,
                fileType: "text/plain"
        )
    }

    ClassificationScheme classificationSchemePayload(){
        new ClassificationScheme(
                label: 'classifiers label',
                description : 'random description',
                readableByEveryone: true,
                readableByAuthenticatedUsers: true)

    }
    Classifier classifierPayload(){
        new Classifier(
            label: 'classifier 1',
            description : 'random description ')

    }

    /**
     * Convenience method for importing a data model into the database for testing
     */

    UUID importDataModel(DataModel dataModelToImport, Folder folder) {
        ExportModel exportModel = ExportModel.build {
            dataModel dataModelToImport
        }
        MultipartBody importRequest = MultipartBody.builder()
            .addPart('folderId', folder.id.toString())
            .addPart('importFile', 'file.json', MediaType.APPLICATION_JSON_TYPE, objectMapper.writeValueAsBytes(exportModel))
            .build()
        String namespace = jsonDataModelImporterPlugin.namespace
        String name = jsonDataModelImporterPlugin.name
        String version = jsonDataModelImporterPlugin.version

        ListResponse<DataModel> response = dataModelApi.importModel(importRequest, namespace, name, version)
        response.items.first().id
    }

    UUID importTerminology(Terminology terminologyToImport, Folder folder) {
        ExportModel exportModel = ExportModel.build {
            terminology terminologyToImport
        }
        MultipartBody importRequest = MultipartBody.builder()
            .addPart('folderId', folder.id.toString())
            .addPart('importFile', 'file.json', MediaType.APPLICATION_JSON_TYPE, objectMapper.writeValueAsBytes(exportModel))
            .build()
        String namespace = jsonTerminologyImporterPlugin.namespace
        String name = jsonTerminologyImporterPlugin.name
        String version = jsonTerminologyImporterPlugin.version

        ListResponse<Terminology> response = terminologyApi.importModel(importRequest, namespace, name, version)
        response.items.first().id
    }

    void setApiKey(UUID apiKey) {
        sessionHandlerClientFilter.apiKey = apiKey
    }

    void unsetApiKey() {
        sessionHandlerClientFilter.apiKey = null
    }


    SubscribedCatalogue mauroJsonSubscribedCataloguePayload() {
        new SubscribedCatalogue(
            url                                  : "https://maurosandbox.com/sandbox",
            subscribedCatalogueType              : SubscribedCatalogueType.MAURO_JSON,
            label                                : 'random label subscribedCatalogue',
            subscribedCatalogueAuthenticationType: SubscribedCatalogueAuthenticationType.API_KEY,
            refreshPeriod                        : 90,
            apiKey                               : 'b39d63d4-4fd4-494d-a491-3c778d89acae'
        )
    }

    SubscribedCatalogue atomSubscribedCataloguePayload(){
        new SubscribedCatalogue(
            url                                  : "http://localhost:8088/test/syndication.xml",
            subscribedCatalogueType              : SubscribedCatalogueType.ATOM,
            label                                : 'atom label',
            subscribedCatalogueAuthenticationType: SubscribedCatalogueAuthenticationType.API_KEY,
            refreshPeriod                        : 90,
            apiKey                               : 'b39d63d4-4fd4-494d-a491-3c778d89acae'
        )
    }

    SubscribedModelFederationParams subscribedModelPayload(UUID folderId) {
        new SubscribedModelFederationParams(subscribedModel:
             new SubscribedModel(subscribedModelId  : '0b97751d-b6bf-476c-a9e6-95d3352e8008',
              subscribedModelType: 'DataModel',
              folderId           : folderId)
        )
    }

    SubscribedModelFederationParams subscribedModelAndImporterProviderServicePayload(UUID folderId) {
        new SubscribedModelFederationParams(subscribedModel:
             new SubscribedModel(subscribedModelId  : '0b97751d-b6bf-476c-a9e6-95d3352e8008',
              subscribedModelType: 'DataModel',
              folderId           : folderId),

         importMetadata: new ImportMetadata(name: 'JsonDataModelImporterPlugin', namespace: 'uk.ac.ox.softeng.mauro.plugin.importer.json', version: '4.0.0')
        )
    }


    SubscribedModelFederationParams subscribedModelAndUrlPayload(UUID folderId, String urlString) {
        new SubscribedModelFederationParams(subscribedModel:
             new SubscribedModel(subscribedModelId  : '0b97751d-b6bf-476c-a9e6-95d3352e8008',
              subscribedModelType: 'DataModel',
              folderId           : folderId),

         url            : urlString
        )
    }

    SubscribedModelFederationParams subscribedModelUrlAndContentTypePayload(UUID folderId, String urlString, String contentType) {
        new SubscribedModelFederationParams(subscribedModel:
             new SubscribedModel(subscribedModelId  : '0b97751d-b6bf-476c-a9e6-95d3352e8008',
              subscribedModelType: 'DataModel',
              folderId           : folderId),

         url            : urlString,
         contentType    : contentType
        )

    }


    SubscribedCatalogue mauroJsonSubscribedCataloguePayload(String label) {
        new SubscribedCatalogue(
            url                                  : "https://maurosandbox.com/sandbox",
            subscribedCatalogueType              : SubscribedCatalogueType.MAURO_JSON,
            label                                : label,
            subscribedCatalogueAuthenticationType: SubscribedCatalogueAuthenticationType.API_KEY,
            refreshPeriod                        : 90,
            apiKey                              : 'b39d63d4-4fd4-494d-a491-3c778d89acae'
        )
    }

    Authority authorityPayload(){
        new Authority(
            url: "http://random.co.uk",
            label: 'authority label'
        )
    }


    FinaliseData finalisePayload() {
        new FinaliseData(versionChangeType: VersionChangeType.MAJOR, versionTag: 'random version tag')
    }
}


