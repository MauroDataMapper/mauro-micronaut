package uk.ac.ox.softeng.mauro.facet


import io.micronaut.runtime.EmbeddedApplication
import io.micronaut.test.annotation.Sql
import jakarta.inject.Inject
import spock.lang.Shared
import uk.ac.ox.softeng.mauro.domain.facet.SummaryMetadata
import uk.ac.ox.softeng.mauro.domain.facet.SummaryMetadataType
import uk.ac.ox.softeng.mauro.domain.folder.Folder
import uk.ac.ox.softeng.mauro.persistence.ContainerizedTest
import uk.ac.ox.softeng.mauro.testing.BaseIntegrationSpec
import uk.ac.ox.softeng.mauro.web.ListResponse

@ContainerizedTest
@Sql(scripts = "classpath:sql/tear-down-core.sql", phase = Sql.Phase.AFTER_EACH)
class SummaryMetadataIntegrationSpec extends BaseIntegrationSpec {

    @Inject
    EmbeddedApplication<? extends EmbeddedApplication> application

    @Shared
    UUID folderId

    UUID summaryMetadataId

    Map<String, String> summaryMetadataMap

    void setupSpec() {
        Folder folder = (Folder) POST("$FOLDERS_PATH", [label: 'Folder with SummaryMetadata'], Folder)
        folderId = folder.id
    }

    void 'list empty SummaryMetadata'() {
        when:
        def response =
        GET("$FOLDERS_PATH/$folderId$SUMMARY_METADATA_PATH", ListResponse<SummaryMetadata>)

        then:
        response.count == 0
    }

    void 'create summaryMetadata'() {
        when:
        summaryMetadataMap = [summaryMetadataType: SummaryMetadataType.STRING]
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
        summaryMetadataMap = [summaryMetadataType: SummaryMetadataType.MAP ]

        (SummaryMetadata) POST("$FOLDERS_PATH/$folderId$SUMMARY_METADATA_PATH", summaryMetadataMap, SummaryMetadata)
        when:
        ListResponse<SummaryMetadata> response = (ListResponse<SummaryMetadata>) GET("$FOLDERS_PATH/$folderId$SUMMARY_METADATA_PATH", ListResponse<SummaryMetadata>)

        then:
        response
        response.count == 1
        response.items.first().summaryMetadataType == SummaryMetadataType.MAP.name()
    }

    void 'get summaryMetadata by Id'() {
        given:
        summaryMetadataMap = [summaryMetadataType: SummaryMetadataType.MAP ]

        SummaryMetadata summaryMetadata = (SummaryMetadata) POST("$FOLDERS_PATH/$folderId$SUMMARY_METADATA_PATH",
                summaryMetadataMap, SummaryMetadata)
        summaryMetadataId = summaryMetadata.id

        when:
        SummaryMetadata saved = GET("$FOLDERS_PATH/$folderId$SUMMARY_METADATA_PATH/$summaryMetadataId",
              SummaryMetadata)

        then:
        saved
        saved.id == summaryMetadataId
        saved.summaryMetadataType == SummaryMetadataType.MAP
    }
}
