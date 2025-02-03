package uk.ac.ox.softeng.mauro.testing

import uk.ac.ox.softeng.mauro.domain.classifier.ClassificationScheme
import uk.ac.ox.softeng.mauro.domain.classifier.Classifier
import uk.ac.ox.softeng.mauro.domain.dataflow.DataFlow
import uk.ac.ox.softeng.mauro.domain.datamodel.DataClass
import uk.ac.ox.softeng.mauro.domain.datamodel.DataModel
import uk.ac.ox.softeng.mauro.domain.datamodel.DataType
import uk.ac.ox.softeng.mauro.domain.facet.Annotation
import uk.ac.ox.softeng.mauro.domain.facet.Metadata
import uk.ac.ox.softeng.mauro.domain.facet.SummaryMetadata
import uk.ac.ox.softeng.mauro.domain.facet.SummaryMetadataType
import uk.ac.ox.softeng.mauro.domain.folder.Folder
import uk.ac.ox.softeng.mauro.domain.model.SummaryMetadataReport

import java.time.Instant


class CommonDataSpec extends BaseIntegrationSpec {
    public static final String REPORT_DATE = "2024-03-01T20:50:01.612Z"

    def codeSet() {
        [
            label       : "Test code set",
            description : "code set description",
            author      : "A.N. Other",
            organisation: "uk.ac.gridpp.ral.org"
        ]
    }

    Folder folder() {
        Folder.build {
            label 'Test folder'
        }
    }


    def terminology() {
        [label: 'Test terminology']
    }

    def termPayload() {
        [code: 'B15.0', definition: 'Hepatitis A with hepatic coma']
    }

    SummaryMetadataReport summaryMetadataReport() {
        new SummaryMetadataReport(reportValue: 'test-report-value', reportDate: Instant.now())
    }

    def ruleRepresentation() {
        [ language: 'java', representation: 'age >= 0']
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

    def rulePayload() {
        [ description: 'My first rule description', name: 'rule name']
    }

    Metadata metadataPayload() {
        Metadata.build {
            namespace 'org.example'
            key 'example_key'
            value 'example_value'
        }
    }

    def dataModelPayload(){
        dataModelPayload("test label ")

    }

    DataModel dataModelPayload(String label){
        new DataModel(label: label, description: 'test description', author: 'test author')
    }

    def dataClassPayload() {
        dataClassPayload('Test data class')
    }

    DataClass dataClassPayload(String label){
        new DataClass(label: label, description: 'test description')
    }

    def dataElementPayload(String label, DataType dataType){
        [label: label, description: 'test description', author: 'test author',
        dataType: dataType]
    }

    DataType dataTypesPayload(){
        new DataType(
            label: 'Test data type',
            dataTypeKind: DataType.DataTypeKind.PRIMITIVE_TYPE,
            units : 'kilograms')
    }
    def dataTypePayload1() {
        [label: 'string', description: 'character string of variable length', domainType: 'PrimitiveType']
    }
    def dataTypePayload2(){
        [label: 'Test data type', domainType: 'primitiveType', units : 'kilograms']
    }

    def termRelationshipType(){
       [label: 'Test Term Relationship Type label', displayLabel: 'Random display label', parentalRelationship: false, childRelationship : false]
    }

    def term(){
        [description : 'Test Term description', code: 'est', definition: 'doloreum-et-val', url : 'https://www.hello.com/test']
    }

    DataFlow dataFlowPayload(UUID sourceId){
        new DataFlow(
            label: 'test label',
            description: 'dataflow payload description ',
            source: new DataModel(id: sourceId))
    }

    def genericModelPayload(String label){
        [ label: label, description: 'test  payload description ' ]
    }

    def referenceFilePayload(){
        String fileContents = 'this is a string file contents'
        [
             fileName: 'reference file name',
             "fileSize": fileContents.size(),
             "fileContents": fileContents.bytes,
             "fileType": "text/plain"
         ]
    }
    def referenceFilePayload(String fileName){
        String fileContents = 'this is a string file contents'
        [
                fileName: fileName,
                "fileSize": fileContents.size(),
                "fileContents": fileContents.bytes,
                "fileType": "text/plain"
        ]
    }
    def referenceFilePayload(String fileName,String content){
        [
                fileName: fileName,
                "fileSize": content.size(),
                "fileContents": content.bytes,
                "fileType": "text/plain"
        ]
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


    def mauroJsonSubscribedCataloguePayload() {
        [
            url                                  : "https://maurosandbox.com/sandbox",
            subscribedCatalogueType              : 'Mauro JSON',
            label                                : 'random label subscribedCatalogue',
            subscribedCatalogueAuthenticationType: 'API Key',
            refreshPeriod                        : 90,
            api_key                              : 'b39d63d4-4fd4-494d-a491-3c778d89acae'
        ]
    }

    def atomSubscribedCataloguePayload(){
        [
            url                                  : "http://localhost:8088/test/syndication.xml",
            subscribedCatalogueType              : 'ATOM',
            label                                : 'atom label',
            subscribedCatalogueAuthenticationType: 'API Key',
            refreshPeriod                        : 90,
            api_key                              : 'b39d63d4-4fd4-494d-a491-3c778d89acae'
        ]
    }

    def subscribedModelPayload(UUID folderId) {
        [subscribedModel:
             [subscribedModelId  : '0b97751d-b6bf-476c-a9e6-95d3352e8008',
              subscribedModelType: 'DataModel',
              folderId           : folderId]
        ]
    }

    def subscribedModelAndImporterProviderServicePayload(UUID folderId) {
        [subscribedModel:
             [subscribedModelId  : '0b97751d-b6bf-476c-a9e6-95d3352e8008',
              subscribedModelType: 'DataModel',
              folderId           : folderId],

         importerProviderService:
             [name: 'JsonDataModelImporterPlugin', namespace: 'uk.ac.ox.softeng.mauro.plugin.importer.json', version: '4.0.0']
        ]
    }


    def subscribedModelAndUrlPayload(UUID folderId, String urlString) {
        [subscribedModel:
             [subscribedModelId  : '0b97751d-b6bf-476c-a9e6-95d3352e8008',
              subscribedModelType: 'DataModel',
              folderId           : folderId],

         url            : urlString
        ]
    }

    def subscribedModelUrlAndContentTypePayload(UUID folderId, String urlString, String contentType) {
        [subscribedModel:
             [subscribedModelId  : '0b97751d-b6bf-476c-a9e6-95d3352e8008',
              subscribedModelType: 'DataModel',
              folderId           : folderId],

         url            : urlString,
         contentType    : contentType
        ]

    }

    def urlPayload() {
        [url: "http://maurosandbox.com/sandbox/api/dataModels/0b97751d-b6bf-476c-a9e6-95d3352e8008/export/uk.ac.ox.softeng.maurodatamapper.datamodel.provider." +
              "exporter/DataModelJsonExporterService/3.2"]
    }


    def mauroJsonSubscribedCataloguePayload(String label) {
        [
            url                                  : "https://maurosandbox.com/sandbox",
            subscribedCatalogueType              : 'Mauro JSON',
            label                                : label,
            subscribedCatalogueAuthenticationType: 'API Key',
            refreshPeriod                        : 90,
            api_key                              : "b39d63d4-4fd4-494d-a491-3c778d89acae"
        ]
    }

    def authorityPayload(){
        [
            url: "http://random.co.uk",
            label: 'authority label'
        ]
    }

}


