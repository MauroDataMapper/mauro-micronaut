package uk.ac.ox.softeng.mauro.testing

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
}
