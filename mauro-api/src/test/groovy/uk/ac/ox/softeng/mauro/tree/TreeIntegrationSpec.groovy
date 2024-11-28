package uk.ac.ox.softeng.mauro.tree

import io.micronaut.http.HttpStatus
import io.micronaut.http.client.exceptions.HttpClientResponseException
import io.micronaut.runtime.EmbeddedApplication
import jakarta.inject.Inject
import spock.lang.Shared
import uk.ac.ox.softeng.mauro.domain.security.SecurableResourceGroupRole
import uk.ac.ox.softeng.mauro.domain.security.UserGroup
import uk.ac.ox.softeng.mauro.domain.tree.TreeItem
import uk.ac.ox.softeng.mauro.persistence.SecuredContainerizedTest
import uk.ac.ox.softeng.mauro.security.SecuredIntegrationSpec

@SecuredContainerizedTest
class TreeIntegrationSpec extends SecuredIntegrationSpec {

    @Inject
    EmbeddedApplication<?> application

    @Shared
    UUID rootFolderId

    @Shared
    UUID folder1Id

    @Shared
    UUID folder2Id

    @Shared
    UUID dataModelId

    @Shared
    UUID dataClassId

    @Shared
    UUID codeSetId

    @Shared
    UUID terminologyId

    @Shared
    UUID userGroupId

    void setupSpec() {
        loginAdmin()
        rootFolderId = UUID.fromString((String) POST('/folders', [label: 'TreeIntegrationSpec root folder']).id)
        folder1Id = UUID.fromString((String) POST("/folders/$rootFolderId/folders", [label: 'TreeIntegrationSpec folder with contents']).id)
        folder2Id = UUID.fromString((String) POST("/folders/$rootFolderId/folders", [label: 'TreeIntegrationSpec empty folder']).id)

        dataModelId = UUID.fromString((String) POST("/folders/$folder1Id/dataModels", [label: 'TreeIntegrationSpec data model']).id)
        dataClassId = UUID.fromString((String) POST("/dataModels/$dataModelId/dataClasses", [label: 'data class']).id)

        terminologyId = UUID.fromString((String) POST("/folders/$folder1Id/terminologies", [label: 'TreeIntegrationSpec terminology']).id)
        codeSetId = UUID.fromString((String) POST("/folders/$folder1Id/codeSets", [label: 'TreeIntegrationSpec code set']).id)
        logout()
    }

    void 'admin can access tree'() {
        when:
        loginAdmin()
        List<Map<String, Object>> tree = GET("/tree/folders?foldersOnly=$foldersOnly", List)

        then:
        tree
        tree.size() >= 1
        tree.find { it.label == 'TreeIntegrationSpec root folder' && it.domainType == 'Folder' && it.hasChildren && UUID.fromString(it.id) == rootFolderId }

        when:
        tree = GET("/tree/folders/$rootFolderId?foldersOnly=$foldersOnly", List)

        then:
        tree
        tree.size() == 2
        tree.find { it.label == 'TreeIntegrationSpec folder with contents' && it.domainType == 'Folder' && it.hasChildren == hasChildren && UUID.fromString(it.id) == folder1Id }
        tree.find { it.label == 'TreeIntegrationSpec empty folder' && it.domainType == 'Folder' && !it.hasChildren  && UUID.fromString(it.id) == folder2Id }

        when:
        List<TreeItem> treeItems = (List<TreeItem>) GET("/tree/folders/$folder1Id?foldersOnly=$foldersOnly", List)

        then:
        if (!treeItems.isEmpty()) {
            treeItems.size() == treeSize
            treeItems.find { it.label == 'TreeIntegrationSpec data model' && it.domainType == 'DataModel' && it.hasChildren && UUID.fromString(it.id) == dataModelId }
            treeItems.find { it.label == 'TreeIntegrationSpec code set' && it.domainType == 'CodeSet' && !it.hasChildren && UUID.fromString(it.id) == codeSetId }
            treeItems.find { it.label == 'TreeIntegrationSpec terminology' && it.domainType == 'Terminology' && !it.hasChildren && UUID.fromString(it.id) == terminologyId }
        }

        when:
        tree = GET("/tree/folders/codeSets/$codeSetId?foldersOnly=$foldersOnly", List)

        then:
        tree.size() == 0

        when:
        tree = GET("/tree/folders/dataModels/$dataModelId?foldersOnly=$foldersOnly", List)

        then:
        tree
        tree.size() == 1
        tree.find { it.label == 'data class' && it.domainType == 'DataClass' && !it.hasChildren && UUID.fromString(it.id) == dataClassId }

        where:
        foldersOnly | hasChildren | treeSize
        null        | true        | 3
        false       | true        | 3
        true        | false       | 0
    }

    void 'non-admin user cannot see tree without permissions'() {
        when:
        loginUser()
        List<Map<String, Object>> tree = GET("/tree/folders?foldersOnly=$foldersOnly", List)

        then:
        !tree.find { it.label == 'TreeIntegrationSpec root folder' }

        when:
        GET("/tree/folders/$rootFolderId?foldersOnly=$foldersOnly", List)

        then:
        HttpClientResponseException exception = thrown()
        exception.status == HttpStatus.FORBIDDEN

        when:
        GET("/tree/folders/dataModels/$dataModelId?foldersOnly=$foldersOnly", List)

        then:
        exception = thrown()
        exception.status == HttpStatus.FORBIDDEN

        where:
        foldersOnly | _
        null        | _
        false       | _
        true        | _
    }

    void 'non-admin user can see the tree when permissions are granted'() {
        given:
        loginAdmin()
        UserGroup readersGroup = (UserGroup) POST('/userGroups', [name: 'Readers Group'], UserGroup)
        userGroupId = readersGroup.id
        PUT("/catalogueUsers/$user.id", [groups: [readersGroup.id]])
        POST("/folder/$rootFolderId/roles/Reader/userGroups/$readersGroup.id", null, SecurableResourceGroupRole)
        logout()

        when:
        loginUser()
        List<Map<String, Object>> tree = GET('/tree/folders', List)

        then:
        tree
        tree.size() >= 1
        tree.find {it.label == 'TreeIntegrationSpec root folder' && it.domainType == 'Folder' && it.hasChildren && UUID.fromString(it.id) == rootFolderId}

        when:
        tree = GET("/tree/folders/$rootFolderId", List)

        then:
        tree
        tree.size() == 2
        tree.find { it.label == 'TreeIntegrationSpec folder with contents' && it.domainType == 'Folder' && it.hasChildren && UUID.fromString(it.id) == folder1Id }
        tree.find { it.label == 'TreeIntegrationSpec empty folder' && it.domainType == 'Folder' && !it.hasChildren && UUID.fromString(it.id) == folder2Id }

        when:
        tree = GET("/tree/folders/$folder1Id", List)

        then:
        tree
        tree.size() == 3
        tree.find { it.label == 'TreeIntegrationSpec data model' && it.domainType == 'DataModel' && it.hasChildren && UUID.fromString(it.id) == dataModelId }
        tree.find { it.label == 'TreeIntegrationSpec code set' && it.domainType == 'CodeSet' && !it.hasChildren && UUID.fromString(it.id) == codeSetId }
        tree.find { it.label == 'TreeIntegrationSpec terminology' && it.domainType == 'Terminology' && !it.hasChildren && UUID.fromString(it.id) == terminologyId }

        when:
        tree = GET("/tree/folders/codeSets/$codeSetId", List)

        then:
        tree.size() == 0

        when:
        tree = GET("/tree/folders/dataModels/$dataModelId", List)

        then:
        tree
        tree.size() == 1
        tree.find {it.label == 'data class' && it.domainType == 'DataClass' && !it.hasChildren && UUID.fromString(it.id) == dataClassId}
    }

    void 'non-admin user cannot see tree when permissions are removed'() {
        given:
        loginAdmin()
        DELETE("/folder/$rootFolderId/roles/Reader/userGroups/$userGroupId", HttpStatus)
        logout()

        when:
        loginUser()
        List<Map<String, Object>> tree = GET("/tree/folders?foldersOnly=true", List)

        then:
        !tree.find { it.label == 'TreeIntegrationSpec root folder' }

        when:
        GET("/tree/folders/$rootFolderId?foldersOnly=true", List)

        then:
        HttpClientResponseException exception = thrown()
        exception.status == HttpStatus.FORBIDDEN

        when:
        GET("/tree/folders/dataModels/$dataModelId?foldersOnly=true", List)

        then:
        exception = thrown()
        exception.status == HttpStatus.FORBIDDEN
    }
}
