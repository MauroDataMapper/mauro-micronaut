package org.maurodata.testing

import org.maurodata.api.SessionHandlerClientFilter
import org.maurodata.api.admin.AdminApi
import org.maurodata.api.authority.AuthorityApi
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
import org.maurodata.api.facet.EditApi
import org.maurodata.api.facet.ReferenceFileApi
import org.maurodata.api.facet.RuleApi
import org.maurodata.api.facet.RuleRepresentationApi
import org.maurodata.api.facet.SemanticLinksApi
import org.maurodata.api.facet.SummaryMetadataApi
import org.maurodata.api.facet.SummaryMetadataReportApi
import org.maurodata.api.federation.PublishApi
import org.maurodata.api.federation.SubscribedCatalogueApi
import org.maurodata.api.federation.SubscribedModelApi
import org.maurodata.api.folder.FolderApi
import org.maurodata.api.folder.VersionedFolderApi
import org.maurodata.api.importer.ImporterApi
import org.maurodata.api.path.PathApi
import org.maurodata.api.profile.ProfileApi
import org.maurodata.api.search.SearchApi
import org.maurodata.api.security.ApiKeyApi
import org.maurodata.api.security.CatalogueUserApi
import org.maurodata.api.security.LoginApi
import org.maurodata.api.security.SecurableResourceGroupRoleApi
import org.maurodata.api.security.UserGroupApi
import org.maurodata.api.security.openidprovider.OpenidProviderApi
import org.maurodata.api.terminology.CodeSetApi
import org.maurodata.api.terminology.TermApi
import org.maurodata.api.terminology.TermRelationshipApi
import org.maurodata.api.terminology.TermRelationshipTypeApi
import org.maurodata.api.terminology.TerminologyApi
import org.maurodata.api.tree.TreeApi
import org.maurodata.domain.authority.Authority
import org.maurodata.domain.classifier.ClassificationScheme
import org.maurodata.domain.classifier.Classifier
import org.maurodata.domain.dataflow.DataFlow
import org.maurodata.domain.datamodel.DataClass
import org.maurodata.domain.datamodel.DataElement
import org.maurodata.domain.datamodel.DataModel
import org.maurodata.domain.datamodel.DataType
import org.maurodata.domain.facet.Annotation
import org.maurodata.domain.facet.Metadata
import org.maurodata.domain.facet.ReferenceFile
import org.maurodata.domain.facet.Rule
import org.maurodata.domain.facet.RuleRepresentation
import org.maurodata.domain.facet.SummaryMetadata
import org.maurodata.domain.facet.SummaryMetadataType
import org.maurodata.domain.facet.federation.SubscribedCatalogue
import org.maurodata.domain.facet.federation.SubscribedCatalogueAuthenticationType
import org.maurodata.domain.facet.federation.SubscribedCatalogueType
import org.maurodata.domain.facet.federation.SubscribedModel
import org.maurodata.domain.facet.federation.SubscribedModelFederationParams
import org.maurodata.domain.folder.Folder
import org.maurodata.domain.facet.SummaryMetadataReport
import org.maurodata.domain.model.version.FinaliseData
import org.maurodata.domain.model.version.VersionChangeType
import org.maurodata.domain.terminology.CodeSet
import org.maurodata.domain.terminology.Term
import org.maurodata.domain.terminology.TermRelationship
import org.maurodata.domain.terminology.TermRelationshipType
import org.maurodata.domain.terminology.Terminology
import org.maurodata.export.ExportModel
import org.maurodata.importdata.ImportMetadata
import org.maurodata.plugin.importer.json.JsonDataModelImporterPlugin
import org.maurodata.plugin.importer.json.JsonTerminologyImporterPlugin
import org.maurodata.web.ListResponse

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
    @Shared @Inject SemanticLinksApi semanticLinksApi
    @Shared @Inject PathApi pathApi
    @Shared @Inject VersionedFolderApi versionedFolderApi

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
    CodeSet codeSet(String label) {
        new CodeSet(
            label: label,
            description: "code set description",
            author: "A.N. Other",
            organisation: "uk.ac.gridpp.ral.org"
        )
    }

    Folder folder() {
        Folder.build {
            label 'Test folder'
        }
    }
    Folder folder(String labelText) {
        Folder.build {
            label  labelText
        }
    }

    Terminology terminologyPayload() {
        new Terminology(label: 'Test terminology')
    }
    Terminology terminologyPayload(String labelText) {
        new Terminology(label: labelText)
    }

    Term termPayload() {
        new Term(code: 'B15.0', definition: 'Hepatitis A with hepatic coma')
    }

    protected Term termPayload(String code, String description, String definition) {
        new Term(description: description,
                 code: code,
                 definition: definition)
    }

    SummaryMetadataReport summaryMetadataReport() {
        new SummaryMetadataReport(reportValue: 'test-report-value', reportDate: REPORT_DATE)
    }

    SummaryMetadataReport summaryMetadataReport(String reportValue, Instant reportDate) {
        new SummaryMetadataReport(reportValue: reportValue, reportDate: reportDate)
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
            dataType: dataType)
    }

    DataType dataTypesPayload(){
        new DataType(
            label: 'Test data type',
            dataTypeKind: DataType.DataTypeKind.PRIMITIVE_TYPE,
            units : 'kilograms')
    }

    DataType dataTypesPayload(String label, DataType.DataTypeKind dataTypeKind){
        new DataType(
            label: label,
            dataTypeKind: dataTypeKind,
            units : 'kilograms')
    }
    TermRelationshipType termRelationshipType(){
       new TermRelationshipType(label: 'Test Term Relationship Type label',
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
    ClassificationScheme classificationSchemePayload(boolean readableByEveryone, boolean readableByAuthenticatedUsers){
        new ClassificationScheme(
            label: 'classification scheme label',
            description : 'random description',
            readableByEveryone: readableByEveryone,
            readableByAuthenticatedUsers: readableByAuthenticatedUsers)

    }
    Classifier classifierPayload(){
        new Classifier(
            label: 'classifier 1',
            description : 'random description ')

    }
    Classifier classifierPayload(String label){
        new Classifier(
            label: label,
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

         importMetadata: new ImportMetadata(name: 'JsonDataModelImporterPlugin', namespace: 'org.maurodata.plugin.importer.json', version: '4.0.0')
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

    DataType referenceTypeDataTypePayload(UUID dataClassId, String label) {
        new DataType(label: label,
                     description: 'Test Reference type description',
                     dataTypeKind: DataType.DataTypeKind.REFERENCE_TYPE,
                     referenceClass: [id: dataClassId])
    }

    DataType modelTypeDataTypePayload(UUID modelId, String modelType) {
        new DataType(label: 'test Model Resource Type',
                     description: 'Test Model resourcde type description',
                     dataTypeKind: DataType.DataTypeKind.MODEL_TYPE,
                     modelResourceDomainType: modelType,
                     modelResourceId: modelId)
    }

    TermRelationship termRelationshipPayload(TermRelationshipType termRelationshipType,
                                             Term source, Term target) {
        new TermRelationship(
            relationshipType: termRelationshipType,
            sourceTerm      : source,
            targetTerm      : target)

    }

     TermRelationshipType termRelationshipTypePayload(String label, boolean aBoolean) {
         new TermRelationshipType(
             label: label,
             childRelationship:  aBoolean)
    }

    DataType enumerationValueDataTypePayload(String label) {
        new DataType(
            label: label,
            dataTypeKind: DataType.DataTypeKind.ENUMERATION_TYPE)
    }

}


