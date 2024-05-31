package uk.ac.ox.softeng.mauro.model

import io.micronaut.http.HttpStatus
import io.micronaut.http.client.exceptions.HttpClientResponseException
import io.micronaut.runtime.EmbeddedApplication
import io.micronaut.test.annotation.Sql
import jakarta.inject.Inject
import spock.lang.Shared
import uk.ac.ox.softeng.mauro.domain.facet.SummaryMetadata
import uk.ac.ox.softeng.mauro.domain.facet.SummaryMetadataType
import uk.ac.ox.softeng.mauro.domain.folder.Folder
import uk.ac.ox.softeng.mauro.domain.model.SummaryMetadataReport
import uk.ac.ox.softeng.mauro.persistence.ContainerizedTest
import uk.ac.ox.softeng.mauro.testing.CommonDataSpec
import uk.ac.ox.softeng.mauro.web.ListResponse

import java.time.Instant

@ContainerizedTest
@Sql(scripts = "classpath:sql/tear-down-summary-metadata.sql", phase = Sql.Phase.AFTER_EACH)
class SummaryMetadataReportIntegrationSpec extends CommonDataSpec {

    @Inject
    EmbeddedApplication<? extends EmbeddedApplication> application

    @Shared
    UUID folderId

    @Shared
    SummaryMetadata summaryMetadata

    void setup() {
        Folder folder = (Folder) POST("$FOLDERS_PATH", [label: 'Folder with SummaryMetadata'], Folder)
        folderId = folder.id
        summaryMetadata = (SummaryMetadata) POST("$FOLDERS_PATH/$folderId$SUMMARY_METADATA_PATH",
               summaryMetadataPayload(), SummaryMetadata)
    }

    void 'list empty SummaryMetadataReport'() {
        when:
        def response =
                GET("$FOLDERS_PATH/$folderId$SUMMARY_METADATA_PATH/$summaryMetadata.id$SUMMARY_METADATA_REPORT_PATH", ListResponse<SummaryMetadataReport>)
        then:
        response.count == 0
    }

    void 'create summaryMetadataReport'() {
        when:
        SummaryMetadataReport summaryMetadataReport = (SummaryMetadataReport) POST("$FOLDERS_PATH/$folderId$SUMMARY_METADATA_PATH/$summaryMetadata.id$SUMMARY_METADATA_REPORT_PATH",
               summaryMetadataReport(), SummaryMetadataReport)

        then:
        summaryMetadataReport
        summaryMetadataReport.id != null
        summaryMetadataReport.domainType == "SummaryMetadataReport"
        summaryMetadataReport.summaryMetadataId == summaryMetadata.id
        summaryMetadataReport.reportDate == Instant.parse(REPORT_DATE)
    }

    void 'list summaryMetadataReport'() {
        given:
        SummaryMetadataReport report1 = (SummaryMetadataReport) POST("$FOLDERS_PATH/$folderId$SUMMARY_METADATA_PATH/$summaryMetadata.id$SUMMARY_METADATA_REPORT_PATH",
                summaryMetadataReport(), SummaryMetadataReport)
        SummaryMetadataReport report2 = (SummaryMetadataReport) POST("$FOLDERS_PATH/$folderId$SUMMARY_METADATA_PATH/$summaryMetadata.id$SUMMARY_METADATA_REPORT_PATH",
               summaryMetadataReport(), SummaryMetadataReport)
        when:
        ListResponse<SummaryMetadataReport> response = (ListResponse<SummaryMetadataReport>) GET("$FOLDERS_PATH/$folderId$SUMMARY_METADATA_PATH/$summaryMetadata.id$SUMMARY_METADATA_REPORT_PATH", ListResponse<SummaryMetadataReport>)

        then:
        response
        response.count == 2
        response.items.id.collect() { it.toString() } == ["$report1.id", "$report2.id"] as List<String>
        response.items.summaryMetadataId.collect().unique() { it.toString() }.size() == 1
        response.items.summaryMetadataId.collect().unique() { it.toString() }[0] == "$summaryMetadata.id"
        response.items.reportDate
    }

    void 'get summaryMetadataReport by Id'() {
        given:
        SummaryMetadataReport report = (SummaryMetadataReport) POST("$FOLDERS_PATH/$folderId$SUMMARY_METADATA_PATH/$summaryMetadata.id$SUMMARY_METADATA_REPORT_PATH",
           summaryMetadataReport(), SummaryMetadataReport)

        when:
        SummaryMetadataReport retrieved = GET("$FOLDERS_PATH/$folderId$SUMMARY_METADATA_PATH/$summaryMetadata.id$SUMMARY_METADATA_REPORT_PATH/$report.id",
                SummaryMetadataReport)

        then:
        retrieved
        retrieved.id == report.id
        retrieved.summaryMetadataId == summaryMetadata.id
        retrieved.reportDate == Instant.parse(REPORT_DATE)
    }

    void 'update summary metadata report by Id'() {
        given:
        SummaryMetadataReport report = (SummaryMetadataReport) POST("$FOLDERS_PATH/$folderId$SUMMARY_METADATA_PATH/$summaryMetadata.id$SUMMARY_METADATA_REPORT_PATH",
                [reportValue: 'test-report-value'], SummaryMetadataReport)

        and:
        def dataAsMap =   [reportValue: 'changed-report-value']

        when:
        SummaryMetadataReport updated = (SummaryMetadataReport) PUT("$FOLDERS_PATH/$folderId$SUMMARY_METADATA_PATH/$summaryMetadata.id$SUMMARY_METADATA_REPORT_PATH/$report.id",
                dataAsMap, SummaryMetadataReport)
        then:
        updated
        updated.reportValue ==  dataAsMap.get('reportValue')
    }

    void 'delete SummaryMetadataReport'() {
        given:
        SummaryMetadataReport report = (SummaryMetadataReport) POST("$FOLDERS_PATH/$folderId$SUMMARY_METADATA_PATH/$summaryMetadata.id$SUMMARY_METADATA_REPORT_PATH",
                [reportValue: 'test-report-value'], SummaryMetadataReport)

        when:
        HttpStatus status = (HttpStatus) DELETE("$FOLDERS_PATH/$folderId$SUMMARY_METADATA_PATH/$summaryMetadata.id$SUMMARY_METADATA_REPORT_PATH/$report.id", HttpStatus)

        then:
        status == HttpStatus.NO_CONTENT
        when:
        SummaryMetadata retrieved = (SummaryMetadata) GET("$FOLDERS_PATH/$folderId$SUMMARY_METADATA_PATH/$summaryMetadata.id", SummaryMetadata)

        then: 'Associated summary metadata is not affected'
        retrieved
        retrieved.id == summaryMetadata.id
        retrieved.domainType == summaryMetadata.domainType
    }

    void 'delete non existing SummaryMetadataReport - should throw exception with http status not found'() {
        when:
        (HttpStatus) DELETE("$FOLDERS_PATH/$folderId$SUMMARY_METADATA_PATH/$summaryMetadata.id$SUMMARY_METADATA_REPORT_PATH/$summaryMetadata.id", HttpStatus)

        then: 'not found exception should be thrown'
        HttpClientResponseException exception = thrown()
        exception.status == HttpStatus.NOT_FOUND

    }

    void 'list summaryMetadata - contains SummaryMetadataReport'() {
        given:
        SummaryMetadataReport report = (SummaryMetadataReport) POST("$FOLDERS_PATH/$folderId$SUMMARY_METADATA_PATH/$summaryMetadata.id$SUMMARY_METADATA_REPORT_PATH",
                summaryMetadataReport(), SummaryMetadataReport)
        when:
        ListResponse<SummaryMetadata> metadataResponse = (ListResponse<SummaryMetadata>) GET("$FOLDERS_PATH/$folderId$SUMMARY_METADATA_PATH", ListResponse<SummaryMetadata>)

        then:
        metadataResponse
        metadataResponse.count == 1
        metadataResponse.items[0].summaryMetadataReports.size() == 1
        metadataResponse.items[0].summaryMetadataReports[0].id == report.id.toString()
    }
}
