package uk.ac.ox.softeng.mauro.facet

import io.micronaut.http.HttpStatus
import io.micronaut.http.client.exceptions.HttpClientResponseException
import io.micronaut.runtime.EmbeddedApplication
import jakarta.inject.Inject
import spock.lang.Shared
import uk.ac.ox.softeng.mauro.domain.facet.Metadata
import uk.ac.ox.softeng.mauro.domain.folder.Folder
import uk.ac.ox.softeng.mauro.persistence.ContainerizedTest
import uk.ac.ox.softeng.mauro.testing.BaseIntegrationSpec
import uk.ac.ox.softeng.mauro.web.ListResponse

@ContainerizedTest
class MetadataIntegrationSpec extends BaseIntegrationSpec {

    @Inject
    EmbeddedApplication<?> application

    @Shared
    UUID folderId

    @Shared
    UUID metadataId

    @Shared
    Map<String, String> metadataMap

    void setupSpec() {
        Folder folder = (Folder) POST('/folders', [label: 'Folder with Metadata'], Folder)
        folderId = folder.id
    }

    void 'list empty metadata'() {
        when:
        ListResponse<Metadata> metadataList = (ListResponse<Metadata>) GET("/folders/$folderId/metadata", ListResponse<Metadata>)

        then:
        metadataList.count == 0
    }

    void 'create metadata'() {
        when:
        metadataMap = [namespace: 'org.example', key: 'example_key', value: 'example_value']
        Metadata metadata = (Metadata) POST("/folders/$folderId/metadata", metadataMap, Metadata)
        metadataId = metadata.id

        then:
        metadata
        metadata.namespace == metadataMap.namespace
        metadata.key == metadataMap.key
        metadata.value == metadataMap.value
    }

    void 'list metadata'() {
        when:
        ListResponse<Metadata> metadataList = (ListResponse<Metadata>) GET("/folders/$folderId/metadata", ListResponse<Metadata>)

        then:
        metadataList
        metadataList.count == 1
        metadataList.items.first().namespace == metadataMap.namespace
        metadataList.items.first().key == metadataMap.key
        metadataList.items.first().value == metadataMap.value
    }

    void 'get metadata by id'() {
        when:
        Metadata metadata = (Metadata) GET("/folders/$folderId/metadata/$metadataId", Metadata)

        then:
        metadata
        metadata.namespace == metadataMap.namespace
        metadata.key == metadataMap.key
        metadata.value == metadataMap.value
    }

    void 'update metadata by id'() {
        when:
        Metadata metadata = (Metadata) PUT("/folders/$folderId/metadata/$metadataId", [value: 'updated'], Metadata)

        then:
        metadata
        metadata.value == 'updated'

        when:
        Metadata metadataUpdated = (Metadata) GET("/folders/$folderId/metadata/$metadataId", Metadata)

        then: 'the show endpoint shows the update'
        metadataUpdated
        metadataUpdated.value == 'updated'

        when:
        ListResponse<Metadata> metadataList = (ListResponse<Metadata>) GET("/folders/$folderId/metadata", ListResponse<Metadata>)

        then: 'the list endpoint shows the update'
        metadataList
        metadataList.count == 1
        metadataList.items.first().value == 'updated'
    }

    void 'delete metadata by id'() {
        when:
        HttpStatus status = (HttpStatus) DELETE("/folders/$folderId/metadata/$metadataId", HttpStatus)

        then:
        status == HttpStatus.NO_CONTENT

        when:
        Metadata metadataDeleted = (Metadata) GET("/folders/$folderId/metadata/$metadataId", Metadata)

        then: 'the show endpoint shows the update'
        HttpClientResponseException exception = thrown()
        exception.status == HttpStatus.NOT_FOUND

        when:
        ListResponse<Metadata> metadataList = (ListResponse<Metadata>) GET("/folders/$folderId/metadata", ListResponse<Metadata>)

        then: 'the list endpoint shows the update'
        metadataList
        metadataList.count == 0
        !metadataList.items
    }
}
