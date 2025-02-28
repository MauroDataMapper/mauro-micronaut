package uk.ac.ox.softeng.mauro.facet

import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpStatus
import io.micronaut.http.client.exceptions.HttpClientResponseException
import io.micronaut.runtime.EmbeddedApplication
import io.micronaut.test.annotation.Sql
import jakarta.inject.Inject
import jakarta.inject.Singleton
import spock.lang.Shared
import uk.ac.ox.softeng.mauro.domain.facet.SummaryMetadata
import uk.ac.ox.softeng.mauro.domain.facet.SummaryMetadataType
import uk.ac.ox.softeng.mauro.domain.folder.Folder
import uk.ac.ox.softeng.mauro.domain.facet.SummaryMetadataReport
import uk.ac.ox.softeng.mauro.persistence.ContainerizedTest
import uk.ac.ox.softeng.mauro.testing.CommonDataSpec
import uk.ac.ox.softeng.mauro.web.ListResponse
import uk.ac.ox.softeng.mauro.web.PaginationParams

@ContainerizedTest
@Singleton
@Sql(scripts = "classpath:sql/tear-down-summary-metadata.sql", phase = Sql.Phase.AFTER_EACH)
class SummaryMetadataIntegrationSpec extends CommonDataSpec {

    @Shared
    UUID folderId

    UUID summaryMetadataId

    void setup() {
        Folder folder = folderApi.create(new Folder(label: 'Folder with SummaryMetadata'))
        folderId = folder.id
    }

    void 'list empty SummaryMetadata'() {
        when:
        ListResponse<SummaryMetadata> response = summaryMetadataApi.list("folder", folderId, new PaginationParams())

        then:
        response.count == 0
    }

    void 'create summaryMetadata'() {
        when:
        SummaryMetadata summaryMetadata = summaryMetadataApi.create("folder", folderId, summaryMetadataPayload())

        then:
        summaryMetadata
        summaryMetadata.id != null
        summaryMetadata.domainType == "SummaryMetadata"
        summaryMetadata.summaryMetadataType == SummaryMetadataType.STRING
    }

    void 'list summaryMetadata'() {
        given:
        summaryMetadataApi.create("folder", folderId, summaryMetadataPayload())

        when:
        ListResponse<SummaryMetadata> response = summaryMetadataApi.list("folder", folderId, new PaginationParams())

        then:
        response
        response.count == 1
        response.items.first().summaryMetadataType == SummaryMetadataType.STRING
    }

    void 'get summaryMetadata by Id'() {
        when:
        SummaryMetadata summaryMetadata = summaryMetadataApi.create("folder", folderId, summaryMetadataPayload())
        summaryMetadataId = summaryMetadata.id

        SummaryMetadata saved = summaryMetadataApi.show("folder", folderId, summaryMetadataId)

        then:
        saved
        saved.id == summaryMetadataId
        saved.summaryMetadataType == SummaryMetadataType.STRING
    }

    void 'update summary metadata'() {
        given:
        SummaryMetadata summaryMetadata = summaryMetadataApi.create("folder", folderId, summaryMetadataPayload())
        summaryMetadataId = summaryMetadata.id

        when:
        SummaryMetadata updated = summaryMetadataApi.update("folder", folderId, summaryMetadataId,
                                                            new SummaryMetadata(summaryMetadataType: SummaryMetadataType.MAP))

        then:
        updated
        updated.summaryMetadataType == SummaryMetadataType.MAP
    }

    void 'delete SummaryMetadata'() {
        given:
        SummaryMetadata summaryMetadata = summaryMetadataApi.create("folder", folderId, summaryMetadataPayload())
        summaryMetadataId = summaryMetadata.id

        when:
        HttpResponse response = summaryMetadataApi.delete("folder", folderId, summaryMetadataId)

        then:
        response.status == HttpStatus.NO_CONTENT

        when:
        summaryMetadata = summaryMetadataApi.show("folder", folderId, summaryMetadataId)

        then: 'the show endpoint shows the update'
        !summaryMetadata

        when:
        ListResponse<SummaryMetadata> responseList = summaryMetadataApi.list("folder", folderId, new PaginationParams())

        then: 'the list endpoint shows the update'
        responseList
        responseList.count == 0
    }

    void 'delete SummaryMetadata with reports'() {
        given:
        SummaryMetadata summaryMetadata = summaryMetadataApi.create("folder", folderId, summaryMetadataPayload())

        and:
        SummaryMetadataReport summaryMetadataReport1 =
            summaryMetadataReportApi.create("folder", folderId, summaryMetadata.id, new SummaryMetadataReport(reportValue: 'test-report-value-1'))
        SummaryMetadataReport summaryMetadataReport2 =
            summaryMetadataReportApi.create("folder", folderId, summaryMetadata.id, new SummaryMetadataReport(reportValue: 'test-report-value-2'))

        ListResponse<SummaryMetadataReport> savedReports = summaryMetadataReportApi.list("folder", folderId, summaryMetadata.id)
        savedReports
        savedReports.count == 2
        savedReports.items.id == List.of(summaryMetadataReport1.id, summaryMetadataReport2.id)

        when:
        HttpResponse response = summaryMetadataApi.delete("folder", folderId, summaryMetadata.id)

        then:
        response.status == HttpStatus.NO_CONTENT

        when:
        SummaryMetadata summaryMetadataResponse = summaryMetadataApi.show("folder", folderId, summaryMetadata.id)

        then: '404 not found is returned, exception thrown'
        !summaryMetadataResponse

        when:
        ListResponse<SummaryMetadataReport> listResponse = summaryMetadataReportApi.list("folder", folderId, summaryMetadata.id)

        then: '404 not found is returned, exception thrown'
        !listResponse
    }
}
