package uk.ac.ox.softeng.mauro.tree

import uk.ac.ox.softeng.mauro.domain.datamodel.DataClass
import uk.ac.ox.softeng.mauro.domain.datamodel.DataModel
import uk.ac.ox.softeng.mauro.domain.folder.Folder
import uk.ac.ox.softeng.mauro.domain.security.CatalogueUser
import uk.ac.ox.softeng.mauro.domain.security.Role
import uk.ac.ox.softeng.mauro.domain.security.SecurableResourceGroupRole
import uk.ac.ox.softeng.mauro.domain.security.UserGroup
import uk.ac.ox.softeng.mauro.domain.terminology.CodeSet
import uk.ac.ox.softeng.mauro.domain.terminology.Terminology
import uk.ac.ox.softeng.mauro.domain.tree.TreeItem
import uk.ac.ox.softeng.mauro.persistence.SecuredContainerizedTest
import uk.ac.ox.softeng.mauro.security.SecuredIntegrationSpec

import io.micronaut.http.HttpStatus
import io.micronaut.http.client.exceptions.HttpClientResponseException
import io.micronaut.runtime.EmbeddedApplication
import io.micronaut.test.annotation.Sql
import jakarta.inject.Inject
import jakarta.inject.Singleton
import spock.lang.Shared
import spock.lang.Unroll

@SecuredContainerizedTest
@Singleton
class TreeFoldersOnlyIntegrationSpec extends SecuredIntegrationSpec {

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
        rootFolderId = folderApi.create(new Folder(label: 'TreeIntegrationSpec root folder')).id
        folder1Id = folderApi.create(rootFolderId, new Folder(label: 'TreeIntegrationSpec folder with contents')).id
        folder2Id = folderApi.create(rootFolderId, new Folder(label: 'TreeIntegrationSpec empty folder')).id

        dataModelId = dataModelApi.create(folder1Id, new DataModel(label: 'TreeIntegrationSpec data model' )).id
        dataClassId = dataClassApi.create(dataModelId, new DataClass(label: 'data class')).id

        terminologyId = terminologyApi.create(folder1Id, new Terminology(label: 'TreeIntegrationSpec terminology')).id
        codeSetId = codeSetApi.create(folder1Id, new CodeSet(label: 'TreeIntegrationSpec code set')).id
        logout()
    }


    @Unroll
    void 'admin can access tree with #foldersOnly'() {
        when:
        loginAdmin()
        List<TreeItem> tree = treeApi.folderTree(null, foldersOnly)

        then:
        tree
        tree.size() >= 1
        tree.find {it.label == 'TreeIntegrationSpec root folder' && it.domainType == 'Folder' && it.hasChildren && it.id == rootFolderId}

        when:
        tree = treeApi.folderTree(rootFolderId, foldersOnly)

        then:
        tree
        tree.size() == 2
        tree.find {it.label == 'TreeIntegrationSpec folder with contents' && it.domainType == 'Folder' && it.hasChildren == hasChildren && it.id == folder1Id}
        tree.find {it.label == 'TreeIntegrationSpec empty folder' && it.domainType == 'Folder' && !it.hasChildren && it.id == folder2Id}

        when:
        List<TreeItem> treeItems = (List<TreeItem>) treeApi.folderTree(folder1Id, foldersOnly)

        then:
        if (!treeItems.isEmpty()) {
            treeItems.size() == treeSize
            treeItems.find {it.label == 'TreeIntegrationSpec data model' && it.domainType == 'DataModel' && it.hasChildren && it.id == dataModelId}
            treeItems.find {it.label == 'TreeIntegrationSpec code set' && it.domainType == 'CodeSet' && !it.hasChildren && it.id == codeSetId}
            treeItems.find {it.label == 'TreeIntegrationSpec terminology' && it.domainType == 'Terminology' && !it.hasChildren && it.id == terminologyId}
        }

        when:
        tree = treeApi.itemTree("codeSet", codeSetId, foldersOnly)

        then:
        tree.size() == 0

        when:
        tree = treeApi.itemTree("dataModel", dataModelId, foldersOnly)

        then:
        tree
        tree.size() == 1
        tree.find {it.label == 'data class' && it.domainType == 'DataClass' && !it.hasChildren && it.id == dataClassId}

        where:
        foldersOnly | hasChildren | treeSize
        null        | true        | 3
        false       | true        | 3
        true        | false       | 0
    }

    @Unroll
    void 'non-admin user cannot see tree with #foldersOnly param without permissions'() {
        when:
        loginUser()
        List<TreeItem> tree = treeApi.folderTree(null, foldersOnly)

        then:
        !tree.find {it.label == 'TreeIntegrationSpec root folder'}

        when:
        treeApi.folderTree(rootFolderId, foldersOnly)

        then:
        HttpClientResponseException exception = thrown()
        exception.status == HttpStatus.FORBIDDEN

        when:
        treeApi.itemTree("dataModel", dataModelId, foldersOnly)

        then:
        exception = thrown()
        exception.status == HttpStatus.FORBIDDEN

        where:
        foldersOnly | _
        null        | _
        false       | _
        true        | _
    }


    void 'non-admin user cannot see tree with #foldersOnly param when permissions are removed'() {
        given:
        loginAdmin()
        UserGroup readersGroup = userGroupApi.create(new UserGroup(name: 'Readers Group'))
        userGroupId = readersGroup.id
        catalogueUserApi.update(user.id, new CatalogueUser(groups: [readersGroup.id] ))
        securableResourceGroupRoleApi.create(
            "folder", rootFolderId, Role.READER, readersGroup.id)

        securableResourceGroupRoleApi.delete(
            "folder", rootFolderId, Role.READER, userGroupId)
        logout()

        when:
        loginUser()
        List<TreeItem> tree = treeApi.folderTree(null, true)
        then:
        !tree.find {it.label == 'TreeIntegrationSpec root folder'}

        when:
        treeApi.folderTree(rootFolderId, true)

        then:
        HttpClientResponseException exception = thrown()
        exception.status == HttpStatus.FORBIDDEN

        when:
        treeApi.itemTree("dataModel", dataModelId, true)

        then:
        exception = thrown()
        exception.status == HttpStatus.FORBIDDEN

        when:
        tree = treeApi.folderTree(null, false)
        then:
        !tree.find {it.label == 'TreeIntegrationSpec root folder'}

        when:
        treeApi.folderTree(rootFolderId, false)

        then:
        exception = thrown()
        exception.status == HttpStatus.FORBIDDEN

        when:
        treeApi.itemTree("dataModel", dataModelId, false)

        then:
        exception = thrown()
        exception.status == HttpStatus.FORBIDDEN

        logout()

    }
}
