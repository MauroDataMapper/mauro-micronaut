package uk.ac.ox.softeng.mauro.facet

import uk.ac.ox.softeng.mauro.api.facet.MetadataApi
import uk.ac.ox.softeng.mauro.api.folder.FolderApi
import uk.ac.ox.softeng.mauro.testing.CommonDataSpec

import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpStatus
import io.micronaut.http.client.exceptions.HttpClientResponseException
import io.micronaut.runtime.EmbeddedApplication
import jakarta.inject.Inject
import jakarta.inject.Singleton
import spock.lang.Shared
import uk.ac.ox.softeng.mauro.domain.facet.Metadata
import uk.ac.ox.softeng.mauro.domain.folder.Folder
import uk.ac.ox.softeng.mauro.persistence.ContainerizedTest
import uk.ac.ox.softeng.mauro.testing.BaseIntegrationSpec
import uk.ac.ox.softeng.mauro.web.ListResponse

@ContainerizedTest
@Singleton
class MetadataIntegrationSpec extends CommonDataSpec {

    @Shared
    UUID folderId

    @Shared
    UUID metadataId

    @Shared
    Map<String, String> metadataMap

    void setup() {
        Folder folder = folderApi.create(new Folder(label: 'Folder with Metadata'))
        folderId = folder.id
    }

    void 'list empty metadata'() {
        when:
        ListResponse<Metadata> metadataList = metadataApi.list("folder", folderId)

        then:
        metadataList.count == 0
    }

    void 'create metadata'() {
        when:
        Metadata metadata1 = new Metadata(namespace: 'org.example', key: 'example_key', value: 'example_value')
        Metadata metadata = metadataApi.create("folder", folderId, metadata1)
        metadataId = metadata.id

        then:
        metadata
        metadata.namespace == metadata1.namespace
        metadata.key == metadata1.key
        metadata.value == metadata1.value
    }

    void 'list metadata'() {
        given:
        Metadata metadata1 = new Metadata(namespace: 'org.example', key: 'example_key', value: 'example_value')
        Metadata metadata = metadataApi.create("folder", folderId, metadata1)
        metadataId = metadata.id

        when:
        ListResponse<Metadata> metadataList = metadataApi.list("folder", folderId)

        then:
        metadataList
        metadataList.count == 1
        metadataList.items.first().namespace == metadata1.namespace
        metadataList.items.first().key == metadata1.key
        metadataList.items.first().value == metadata1.value
    }

    void 'get metadata by id'() {
        given:
        Metadata metadata1 = new Metadata(namespace: 'org.example', key: 'example_key', value: 'example_value')
        Metadata metadata = metadataApi.create("folder", folderId, metadata1)
        metadataId = metadata.id

        when:
        Metadata retrieved = metadataApi.show("folder", folderId, metadataId)

        then:
        retrieved
        retrieved.namespace == metadata1.namespace
        retrieved.key == metadata1.key
        retrieved.value == metadata1.value
    }

    void 'update metadata by id'() {
        given:
        Metadata metadata1 = new Metadata(namespace: 'org.example', key: 'example_key', value: 'example_value')
        Metadata metadata = metadataApi.create("folder", folderId, metadata1)
        metadataId = metadata.id

        when:
        Metadata saved = metadataApi.update("folder", folderId, metadataId,new Metadata(value: 'updated'))

        then:
        saved
        saved.value == 'updated'

        when:
        Metadata metadataUpdated = metadataApi.show("folder", folderId, metadataId)

        then: 'the show endpoint shows the update'
        metadataUpdated
        metadataUpdated.value == 'updated'

        when:
        ListResponse<Metadata> metadataList = metadataApi.list("folder", folderId)

        then: 'the list endpoint shows the update'
        metadataList
        metadataList.count == 1
        metadataList.items.first().value == 'updated'
    }

    void 'delete metadata by id'() {
        given:
        Metadata metadata1 = new Metadata(namespace: 'org.example', key: 'example_key', value: 'example_value')
        Metadata metadata = metadataApi.create("folder", folderId, metadata1)
        metadataId = metadata.id

        when:
        HttpResponse response = metadataApi.delete("folder", folderId, metadataId)

        then:
        response.status == HttpStatus.NO_CONTENT

        when:
        Metadata metadata2 = metadataApi.show("folder", folderId, metadataId)

        then: 'the show endpoint shows the update'
        !metadata2

        when:
        ListResponse<Metadata> metadataList = metadataApi.list("folder", folderId,)

        then: 'the list endpoint shows the update'
        metadataList
        metadataList.count == 0
        !metadataList.items
    }
}
