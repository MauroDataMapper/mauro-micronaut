package uk.ac.ox.softeng.mauro.facet

import uk.ac.ox.softeng.mauro.domain.facet.SummaryMetadata
import uk.ac.ox.softeng.mauro.domain.facet.SummaryMetadataReport
import uk.ac.ox.softeng.mauro.domain.folder.Folder
import uk.ac.ox.softeng.mauro.persistence.ContainerizedTest
import uk.ac.ox.softeng.mauro.testing.CommonDataSpec
import uk.ac.ox.softeng.mauro.web.ListResponse

import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpStatus
import io.micronaut.test.annotation.Sql
import jakarta.inject.Singleton
import spock.lang.Shared

@ContainerizedTest
@Singleton
@Sql(scripts = "classpath:sql/tear-down-summary-metadata.sql", phase = Sql.Phase.AFTER_EACH)
class SummaryMetadataReportIntegrationSpec extends CommonDataSpec {

    @Shared
    UUID folderId

    @Shared
    SummaryMetadata summaryMetadata

    void setup() {
        Folder folder = folderApi.create(new Folder(label: 'Folder with SummaryMetadata'))
        folderId = folder.id
        summaryMetadata = summaryMetadataApi.create("folder", folderId, summaryMetadataPayload())
    }

    void 'list empty SummaryMetadataReport'() {
        when:
        ListResponse<SummaryMetadataReport> response =
            summaryMetadataReportApi.list("folder", folderId, summaryMetadata.id)
        then:
        response.count == 0
    }

    void 'create summaryMetadataReport'() {
        when:
        SummaryMetadataReport summaryMetadataReport =
            summaryMetadataReportApi.create("folder", folderId, summaryMetadata.id, summaryMetadataReport())

        then:
        summaryMetadataReport
        summaryMetadataReport.id != null
        summaryMetadataReport.domainType == "SummaryMetadataReport"
        summaryMetadataReport.summaryMetadataId == summaryMetadata.id
        summaryMetadataReport.reportDate == REPORT_DATE
    }

    void 'list summaryMetadataReport'() {
        given:
        SummaryMetadataReport report1 = summaryMetadataReportApi.create(
            "folder", folderId, summaryMetadata.id, summaryMetadataReport())
        SummaryMetadataReport report2 = summaryMetadataReportApi.create(
            "folder", folderId, summaryMetadata.id, summaryMetadataReport())
        when:
        ListResponse<SummaryMetadataReport> response =
            summaryMetadataReportApi.list("folder", folderId, summaryMetadata.id)

        then:
        response
        response.count == 2
        response.items.id.collect().sort() == [report1.id, report2.id].sort()
        response.items.summaryMetadataId.collect().unique { it.toString() }.size() == 1
        response.items.summaryMetadataId.collect().unique { it.toString() }[0].toString() == "$summaryMetadata.id"
        response.items.reportDate
    }

    void 'get summaryMetadataReport by Id'() {
        given:
        SummaryMetadataReport report = summaryMetadataReportApi.create(
            "folder", folderId, summaryMetadata.id, summaryMetadataReport())

        when:
        SummaryMetadataReport retrieved = summaryMetadataReportApi.show("folder", folderId, summaryMetadata.id, report.id)

        then:
        retrieved
        retrieved.id == report.id
        retrieved.summaryMetadataId == summaryMetadata.id
        retrieved.reportDate == REPORT_DATE
    }

    void 'update summary metadata report by Id'() {
        given:
        SummaryMetadataReport report = summaryMetadataReportApi.create(
            "folder", folderId, summaryMetadata.id, new SummaryMetadataReport(reportValue: 'test-report-value'))

        when:
        SummaryMetadataReport updated = summaryMetadataReportApi.update(
            "folder", folderId, summaryMetadata.id, report.id, new SummaryMetadataReport(reportValue: 'changed-report-value'))
        then:
        updated
        updated.reportValue ==  'changed-report-value'
    }

    void 'delete SummaryMetadataReport'() {
        given:
        SummaryMetadataReport report = summaryMetadataReportApi.create(
            "folder", folderId, summaryMetadata.id, new SummaryMetadataReport(reportValue: 'test-report-value'))

        when:
        HttpResponse response = summaryMetadataReportApi.delete("folder", folderId, summaryMetadata.id, report.id)

        then:
        response.status == HttpStatus.NO_CONTENT

        when:
        SummaryMetadata retrieved = summaryMetadataApi.show("folder", folderId, summaryMetadata.id)

        then: 'Associated summary metadata is not affected'
        retrieved
        retrieved.id == summaryMetadata.id
        retrieved.domainType == summaryMetadata.domainType
    }

    void 'delete non existing SummaryMetadataReport - should throw exception with http status not found'() {
        when:
        HttpResponse response = summaryMetadataReportApi.delete("folder", folderId, summaryMetadata.id, summaryMetadata.id)

        then: 'not found exception should be thrown'
        response.status == HttpStatus.NOT_FOUND

    }

    void 'list summaryMetadata - contains SummaryMetadataReport'() {
        given:
        SummaryMetadataReport report = summaryMetadataReportApi.create(
            "folder", folderId, summaryMetadata.id, summaryMetadataReport())
        when:
        ListResponse<SummaryMetadata> metadataResponse = summaryMetadataApi.list("folder", folderId)

        then:
        metadataResponse
        metadataResponse.count == 1
        metadataResponse.items[0].summaryMetadataReports.size() == 1
        metadataResponse.items[0].summaryMetadataReports[0].id == report.id
    }
}
