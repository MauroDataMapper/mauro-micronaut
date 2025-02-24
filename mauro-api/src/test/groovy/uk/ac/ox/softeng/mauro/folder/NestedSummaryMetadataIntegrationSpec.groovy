package uk.ac.ox.softeng.mauro.folder

import io.micronaut.runtime.EmbeddedApplication
import io.micronaut.test.annotation.Sql
import jakarta.inject.Inject
import spock.lang.Shared
import uk.ac.ox.softeng.mauro.domain.facet.SummaryMetadata
import uk.ac.ox.softeng.mauro.domain.folder.Folder
import uk.ac.ox.softeng.mauro.domain.facet.SummaryMetadataReport
import uk.ac.ox.softeng.mauro.persistence.ContainerizedTest
import uk.ac.ox.softeng.mauro.testing.CommonDataSpec
import uk.ac.ox.softeng.mauro.web.ListResponse

@ContainerizedTest
@Sql(scripts = "classpath:sql/tear-down-summary-metadata.sql", phase = Sql.Phase.AFTER_EACH)
class NestedSummaryMetadataIntegrationSpec extends CommonDataSpec {

    @Inject
    EmbeddedApplication<? extends EmbeddedApplication> application

    @Shared
    UUID folderId

    UUID summaryMetadataId


    void setup() {
        Folder folder = (Folder) POST("$FOLDERS_PATH", [label: 'Folder with SummaryMetadata'], Folder)
        folderId = folder.id
    }


    void 'create summaryMetadata - folder should show created object'() {
        given:
        Folder retrieved = (Folder) GET("$FOLDERS_PATH/$folderId", Folder)
        !retrieved.summaryMetadata

        and:
        SummaryMetadata summaryMetadata = (SummaryMetadata) POST("$FOLDERS_PATH/$folderId$SUMMARY_METADATA_PATH",
                summaryMetadataPayload(), SummaryMetadata)

        when:
        retrieved = (Folder) GET("$FOLDERS_PATH/$folderId", Folder)

        then:
        retrieved.summaryMetadata
        retrieved.summaryMetadata.size() == 1
        retrieved.summaryMetadata[0].id == summaryMetadata.id
    }


    void 'create summaryMetadata with reports  - folder should show created nested objects'() {
        given:
        Folder retrieved = (Folder) GET("$FOLDERS_PATH/$folderId", Folder)
        !retrieved.summaryMetadata

        and:
        SummaryMetadata summaryMetadata = (SummaryMetadata) POST("$FOLDERS_PATH/$folderId$SUMMARY_METADATA_PATH",
                summaryMetadataPayload(), SummaryMetadata)
        UUID summaryMetadataId = summaryMetadata.id

        and:
        SummaryMetadataReport summaryMetadataReport = (SummaryMetadataReport) POST("$FOLDERS_PATH/$folderId$SUMMARY_METADATA_PATH/$summaryMetadataId$SUMMARY_METADATA_REPORT_PATH",
                [reportValue: 'test-report-value'], SummaryMetadataReport)


        when:
        retrieved = (Folder) GET("$FOLDERS_PATH/$folderId", Folder)

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
        Folder retrieved = (Folder) GET("$FOLDERS_PATH/$folderId", Folder)
        !retrieved.summaryMetadata

        and:
        SummaryMetadata summaryMetadata = (SummaryMetadata) POST("$FOLDERS_PATH/$folderId$SUMMARY_METADATA_PATH",
              summaryMetadataPayload(), SummaryMetadata)
        UUID summaryMetadataId = summaryMetadata.id

        and:
        SummaryMetadataReport summaryMetadataReport1 = (SummaryMetadataReport) POST("$FOLDERS_PATH/$folderId$SUMMARY_METADATA_PATH/$summaryMetadataId$SUMMARY_METADATA_REPORT_PATH",
                [reportValue: 'test-report-value-1'], SummaryMetadataReport)

        SummaryMetadataReport summaryMetadataReport2 = (SummaryMetadataReport) POST("$FOLDERS_PATH/$folderId$SUMMARY_METADATA_PATH/$summaryMetadataId$SUMMARY_METADATA_REPORT_PATH",
                [reportValue: 'test-report-value-2'], SummaryMetadataReport)

        when:
        SummaryMetadata saved = (SummaryMetadata) GET("$FOLDERS_PATH/$folderId$SUMMARY_METADATA_PATH/$summaryMetadataId", SummaryMetadata)

        then:
        saved
        saved.id == summaryMetadataId

        when:
        ListResponse<SummaryMetadataReport> savedReports = (ListResponse<SummaryMetadataReport>) GET("$FOLDERS_PATH/$folderId$SUMMARY_METADATA_PATH/$summaryMetadata.id$SUMMARY_METADATA_REPORT_PATH",
                ListResponse, SummaryMetadataReport)

        then:
        savedReports
        savedReports.count == 2
        savedReports.items.id == List.of(summaryMetadataReport1.id, summaryMetadataReport2.id)

        when:
        SummaryMetadata savedSummaryMetadata = (SummaryMetadata) GET("$FOLDERS_PATH/$folderId$SUMMARY_METADATA_PATH/$summaryMetadata.id", SummaryMetadata)

        then:
        savedSummaryMetadata
        savedSummaryMetadata.summaryMetadataReports
        savedSummaryMetadata.summaryMetadataReports.size() == 2
        savedSummaryMetadata.summaryMetadataReports.id == List.of(summaryMetadataReport1.id, summaryMetadataReport2.id)
    }
}
