package uk.ac.ox.softeng.mauro.facet

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
import uk.ac.ox.softeng.mauro.testing.BaseIntegrationSpec
import uk.ac.ox.softeng.mauro.testing.CommonDataSpec
import uk.ac.ox.softeng.mauro.web.ListResponse

@ContainerizedTest
@Sql(scripts = "classpath:sql/tear-down-summary-metadata.sql", phase = Sql.Phase.AFTER_EACH)
class SummaryMetadataIntegrationSpec extends CommonDataSpec {

    @Inject
    EmbeddedApplication<? extends EmbeddedApplication> application

    @Shared
    UUID folderId

    UUID summaryMetadataId

    Map<String, String> summaryMetadataMap

    void setup() {
        Folder folder = (Folder) POST("$FOLDERS_PATH", [label: 'Folder with SummaryMetadata'], Folder)
        folderId = folder.id
    }

    void 'list empty SummaryMetadata'() {
        when:
        def response =
                GET("$FOLDERS_PATH/$folderId$SUMMARY_METADATA_PATH", ListResponse, SummaryMetadata)

        then:
        response.count == 0
    }

    void 'create summaryMetadata'() {
        given:
        summaryMetadataMap = summaryMetadataPayload()

        when:
        SummaryMetadata summaryMetadata = (SummaryMetadata) POST("$FOLDERS_PATH/$folderId$SUMMARY_METADATA_PATH",
                summaryMetadataMap, SummaryMetadata)

        then:
        summaryMetadata
        summaryMetadata.id != null
        summaryMetadata.domainType == "SummaryMetadata"
        summaryMetadata.summaryMetadataType == summaryMetadataMap.summaryMetadataType
    }

    void 'list summaryMetadata'() {
        given:
        summaryMetadataMap = summaryMetadataPayload()
        and:
        (SummaryMetadata) POST("$FOLDERS_PATH/$folderId$SUMMARY_METADATA_PATH", summaryMetadataMap, SummaryMetadata)
        when:
        ListResponse<SummaryMetadata> response = (ListResponse<SummaryMetadata>) GET("$FOLDERS_PATH/$folderId$SUMMARY_METADATA_PATH", ListResponse, SummaryMetadata)

        then:
        response
        response.count == 1
        response.items.first().summaryMetadataType as String == SummaryMetadataType.STRING.name()
    }

    void 'get summaryMetadata by Id'() {
        given:
        summaryMetadataMap = summaryMetadataPayload()

        and:
        SummaryMetadata summaryMetadata = (SummaryMetadata) POST("$FOLDERS_PATH/$folderId$SUMMARY_METADATA_PATH",
                summaryMetadataMap, SummaryMetadata)
        summaryMetadataId = summaryMetadata.id

        when:
        SummaryMetadata saved = GET("$FOLDERS_PATH/$folderId$SUMMARY_METADATA_PATH/$summaryMetadataId",
                SummaryMetadata)

        then:
        saved
        saved.id == summaryMetadataId
        saved.summaryMetadataType == SummaryMetadataType.STRING
    }

    void 'update summary metadata'() {
        given:
        summaryMetadataMap = summaryMetadataPayload()

        and:
        SummaryMetadata summaryMetadata = (SummaryMetadata) POST("$FOLDERS_PATH/$folderId$SUMMARY_METADATA_PATH",
                summaryMetadataMap, SummaryMetadata)
        summaryMetadataId = summaryMetadata.id

        when:
        SummaryMetadata updated = (SummaryMetadata) PUT("$FOLDERS_PATH/$folderId$SUMMARY_METADATA_PATH/$summaryMetadataId",
                [summaryMetadataType: SummaryMetadataType.STRING], SummaryMetadata)

        then:
        updated
        updated.summaryMetadataType == SummaryMetadataType.STRING
    }

    void 'delete SummaryMetadata'() {
        given:
        summaryMetadataMap = summaryMetadataPayload()
        and:
        SummaryMetadata summaryMetadata = (SummaryMetadata) POST("$FOLDERS_PATH/$folderId$SUMMARY_METADATA_PATH",
                summaryMetadataMap, SummaryMetadata)
        summaryMetadataId = summaryMetadata.id

        when:
        HttpStatus status = (HttpStatus) DELETE("$FOLDERS_PATH/$folderId$SUMMARY_METADATA_PATH/$summaryMetadataId", HttpStatus)

        then:
        status == HttpStatus.NO_CONTENT

        when:
        (SummaryMetadata) GET("$FOLDERS_PATH/$folderId$SUMMARY_METADATA_PATH/$summaryMetadataId", SummaryMetadata)

        then: 'the show endpoint shows the update'
        HttpClientResponseException exception = thrown()
        exception.status == HttpStatus.NOT_FOUND

        when:
        ListResponse<SummaryMetadata> response = (ListResponse<SummaryMetadata>) GET("$FOLDERS_PATH/$folderId$SUMMARY_METADATA_PATH", ListResponse, SummaryMetadata)

        then: 'the list endpoint shows the update'
        response
        response.count == 0
    }

    void 'delete SummaryMetadata with reports'() {
        given:
        SummaryMetadata summaryMetadata = (SummaryMetadata) POST("$FOLDERS_PATH/$folderId$SUMMARY_METADATA_PATH",
                summaryMetadataPayload(), SummaryMetadata)

        and:
        SummaryMetadataReport summaryMetadataReport1 = (SummaryMetadataReport) POST("$FOLDERS_PATH/$folderId$SUMMARY_METADATA_PATH/$summaryMetadata.id$SUMMARY_METADATA_REPORT_PATH",
                [reportValue: 'test-report-value-1'], SummaryMetadataReport)
        SummaryMetadataReport summaryMetadataReport2 = (SummaryMetadataReport) POST("$FOLDERS_PATH/$folderId$SUMMARY_METADATA_PATH/$summaryMetadata.id$SUMMARY_METADATA_REPORT_PATH",
                [reportValue: 'test-report-value-2'], SummaryMetadataReport)

        ListResponse<SummaryMetadataReport> savedReports = (ListResponse<SummaryMetadataReport>) GET("$FOLDERS_PATH/$folderId$SUMMARY_METADATA_PATH/$summaryMetadata.id$SUMMARY_METADATA_REPORT_PATH",
                ListResponse, SummaryMetadataReport)
        savedReports
        savedReports.count == 2
        savedReports.items.id == List.of(summaryMetadataReport1.id, summaryMetadataReport2.id)

        when:
        HttpStatus status = (HttpStatus) DELETE("$FOLDERS_PATH/$folderId$SUMMARY_METADATA_PATH/$summaryMetadata.id", HttpStatus)

        then:
        status == HttpStatus.NO_CONTENT

        when:
        (SummaryMetadata) GET("$FOLDERS_PATH/$folderId$SUMMARY_METADATA_PATH/$summaryMetadata.id", SummaryMetadata)

        then: '404 not found is returned, exception thrown'
        HttpClientResponseException exception = thrown()
        exception.status == HttpStatus.NOT_FOUND

        when:
        (ListResponse<SummaryMetadataReport>) GET("$FOLDERS_PATH/$folderId$SUMMARY_METADATA_PATH/$summaryMetadata.id$SUMMARY_METADATA_REPORT_PATH",
                ListResponse, SummaryMetadataReport)

        then: '404 not found is returned, exception thrown'
        exception = thrown()
        exception.status == HttpStatus.NOT_FOUND
    }
}
