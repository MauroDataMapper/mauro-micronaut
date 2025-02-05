package uk.ac.ox.softeng.mauro.folder

import io.micronaut.runtime.EmbeddedApplication
import io.micronaut.test.annotation.Sql
import jakarta.inject.Inject
import jakarta.inject.Singleton
import spock.lang.Shared
import uk.ac.ox.softeng.mauro.domain.facet.SummaryMetadata
import uk.ac.ox.softeng.mauro.domain.folder.Folder
import uk.ac.ox.softeng.mauro.domain.facet.SummaryMetadataReport
import uk.ac.ox.softeng.mauro.persistence.ContainerizedTest
import uk.ac.ox.softeng.mauro.testing.CommonDataSpec
import uk.ac.ox.softeng.mauro.web.ListResponse

@ContainerizedTest
@Singleton
@Sql(scripts = "classpath:sql/tear-down-summary-metadata.sql", phase = Sql.Phase.AFTER_EACH)
class NestedSummaryMetadataIntegrationSpec extends CommonDataSpec {

    @Shared
    UUID folderId

    UUID summaryMetadataId


    void setup() {
        Folder folder = folderApi.create(new Folder(label: 'Folder with SummaryMetadata'))
        folderId = folder.id
    }


    void 'create summaryMetadata - folder should show created object'() {
        given:
        Folder retrieved = folderApi.show(folderId)
        !retrieved.summaryMetadata

        and:
        SummaryMetadata summaryMetadata = summaryMetadataApi.create("folder", folderId, summaryMetadataPayload())

        when:
        retrieved = folderApi.show(folderId)

        then:
        retrieved.summaryMetadata
        retrieved.summaryMetadata.size() == 1
        retrieved.summaryMetadata[0].id == summaryMetadata.id
    }


    void 'create summaryMetadata with reports  - folder should show created nested objects'() {
        given:
        Folder retrieved = folderApi.show(folderId)
        !retrieved.summaryMetadata

        and:
        SummaryMetadata summaryMetadata = summaryMetadataApi.create("folder", folderId, summaryMetadataPayload())
        UUID summaryMetadataId = summaryMetadata.id

        and:
        SummaryMetadataReport summaryMetadataReport = summaryMetadataReportApi.create("folder", folderId, summaryMetadataId,
                new SummaryMetadataReport(reportValue: 'test-report-value'))


        when:
        retrieved = folderApi.show(folderId)

        then:
        retrieved.summaryMetadata
        retrieved.summaryMetadata.size() == 1
        SummaryMetadata retrievedSummaryMetadata = retrieved.summaryMetadata[0]
        retrievedSummaryMetadata.id == summaryMetadataId
        retrievedSummaryMetadata.summaryMetadataReports
        retrievedSummaryMetadata.summaryMetadataReports.size() == 1
        retrievedSummaryMetadata.summaryMetadataReports[0].id == summaryMetadataReport.id
    }

    void 'create summaryMetadata with reports  - summaryMetadata should show created reports'() {
        given:
        Folder retrieved = folderApi.show(folderId)
        !retrieved.summaryMetadata

        and:
        SummaryMetadata summaryMetadata = summaryMetadataApi.create("folder", folderId, summaryMetadataPayload())
        UUID summaryMetadataId = summaryMetadata.id

        and:
        SummaryMetadataReport summaryMetadataReport1 = summaryMetadataReportApi.create("folder", folderId, summaryMetadataId,
            new SummaryMetadataReport(reportValue: 'test-report-value-1'))

        SummaryMetadataReport summaryMetadataReport2 = summaryMetadataReportApi.create("folder", folderId, summaryMetadataId,
            new SummaryMetadataReport(reportValue: 'test-report-value-2'))

        when:
        SummaryMetadata saved = summaryMetadataApi.show("folder", folderId, summaryMetadataId)

        then:
        saved
        saved.id == summaryMetadataId

        when:
        ListResponse<SummaryMetadataReport> savedReports = summaryMetadataReportApi.list("folder", folderId, summaryMetadata.id)

        then:
        savedReports
        savedReports.count == 2
        savedReports.items.id == List.of(summaryMetadataReport1.id, summaryMetadataReport2.id)

        when:
        SummaryMetadata savedSummaryMetadata = summaryMetadataApi.show("folder", folderId, summaryMetadata.id)

        then:
        savedSummaryMetadata
        savedSummaryMetadata.summaryMetadataReports
        savedSummaryMetadata.summaryMetadataReports.size() == 2
        savedSummaryMetadata.summaryMetadataReports.id == List.of(summaryMetadataReport1.id, summaryMetadataReport2.id)
    }
}
