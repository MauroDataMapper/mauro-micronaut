package uk.ac.ox.softeng.mauro.testing

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
        [label: 'Test data model', description: 'test description', author: 'test author']
    }

    def dataClassPayload(){
        [label: 'Test data class', description: 'test description', author: 'test author']
    }

    def dataTypesPayload(){
        [label: 'Test data type', domainType: 'primitiveType', units : 'kilograms']
    }
}
