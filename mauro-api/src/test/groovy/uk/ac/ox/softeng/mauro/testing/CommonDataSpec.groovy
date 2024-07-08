package uk.ac.ox.softeng.mauro.testing

import uk.ac.ox.softeng.mauro.domain.datamodel.DataType
import uk.ac.ox.softeng.mauro.domain.facet.SummaryMetadataType


class CommonDataSpec extends BaseIntegrationSpec{
    public static final String REPORT_DATE = "2024-03-01T20:50:01.612Z"

    def codeSet() {
        [
                label       : "Test code set",
                description : "code set description",
                author      : "A.N. Other",
                organisation: "uk.ac.gridpp.ral.org"
        ]
    }

    def folder() {
        [label: 'Test folder']
    }


    def terminology() {
        [label: 'Test terminology']
    }

    def termPayload() {
        [code: 'B15.0', definition: 'Hepatitis A with hepatic coma']
    }
    def summaryMetadataReport() {
        [ reportValue: 'test-report-value', reportDate: REPORT_DATE]
    }
    def annotationPayload() {
        [label: 'test-label', description: 'test-annotation description']
    }
    def annotationPayload(String label, String description) {
        [label: label, description: description ]
    }
    def summaryMetadataPayload() {
        [ summaryMetadataType: SummaryMetadataType.STRING, label: 'summary metadata label']
    }
    def metadataPayload() {
        [ namespace: 'org.example', key: 'example_key', value: 'example_value']
    }

    def dataModelPayload(){
        dataModelPayload("test label ")

    }
    def dataModelPayload(String label){
        [label: label, description: 'test description', author: 'test author']
    }
    def dataClassPayload() {
        dataClassPayload('Test data class')
    }

    def dataClassPayload(String label){
        [label: label, description: 'test description', author: 'test author']
    }

    def dataElementPayload(String label, DataType dataType){
        [label: label, description: 'test description', author: 'test author',
        dataType: dataType]
    }

    def dataTypesPayload(){
        [label: 'Test data type', domainType: 'primitiveType', units : 'kilograms']
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

    def dataFlowPayload(String sourceId){
        [source: [id: sourceId], label: 'test label', description: 'dataflow payload description ' ]
    }


}
