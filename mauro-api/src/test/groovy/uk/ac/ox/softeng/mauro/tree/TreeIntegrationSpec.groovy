package uk.ac.ox.softeng.mauro.tree

import uk.ac.ox.softeng.mauro.domain.classifier.ClassificationScheme
import uk.ac.ox.softeng.mauro.domain.datamodel.DataClass
import uk.ac.ox.softeng.mauro.domain.datamodel.DataModel
import uk.ac.ox.softeng.mauro.domain.folder.Folder
import uk.ac.ox.softeng.mauro.domain.security.CatalogueUser
import uk.ac.ox.softeng.mauro.domain.security.Role
import uk.ac.ox.softeng.mauro.domain.security.UserGroup
import uk.ac.ox.softeng.mauro.domain.terminology.CodeSet
import uk.ac.ox.softeng.mauro.domain.terminology.Terminology
import uk.ac.ox.softeng.mauro.domain.tree.TreeItem
import uk.ac.ox.softeng.mauro.persistence.SecuredContainerizedTest
import uk.ac.ox.softeng.mauro.security.SecuredIntegrationSpec

import io.micronaut.http.HttpStatus
import io.micronaut.http.client.exceptions.HttpClientResponseException
import jakarta.inject.Singleton
import spock.lang.Shared

@SecuredContainerizedTest
@Singleton
class TreeIntegrationSpec extends SecuredIntegrationSpec {

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

    @Shared
    UUID classificationSchemeId

    void setupSpec() {
        loginAdmin()
        rootFolderId = folderApi.create(new Folder(label: 'TreeIntegrationSpec root folder')).id
        folder1Id = folderApi.create(rootFolderId, new Folder(label: 'TreeIntegrationSpec folder with contents')).id
        folder2Id = folderApi.create(rootFolderId, new Folder(label: 'TreeIntegrationSpec empty folder')).id

        dataModelId = dataModelApi.create(folder1Id, new DataModel(label: 'TreeIntegrationSpec data model')).id
        dataClassId = dataClassApi.create(dataModelId, new DataClass(label: 'data class')).id

        terminologyId = terminologyApi.create(folder1Id, new Terminology(label: 'TreeIntegrationSpec terminology')).id
        codeSetId = codeSetApi.create(folder1Id, new CodeSet(label: 'TreeIntegrationSpec code set')).id
        classificationSchemeId = classificationSchemeApi.create(folder1Id,  new ClassificationScheme(label: 'TreeIntegrationSpec classification scheme')).id
        logout()
    }

    void 'admin can access tree'() {
        when:
        loginAdmin()
        List<TreeItem> tree = treeApi.folderTree(null, false)

        then:
        tree
        tree.size() >= 1
        tree.find {it.label == 'TreeIntegrationSpec root folder' && it.domainType == 'Folder' && it.hasChildren && it.id == rootFolderId}

        when:
        tree = treeApi.folderTree(rootFolderId, false)

        then:
        tree
        tree.size() == 2
        tree.find {it.label == 'TreeIntegrationSpec folder with contents' && it.domainType == 'Folder' && it.hasChildren && it.id == folder1Id}
        tree.find {it.label == 'TreeIntegrationSpec empty folder' && it.domainType == 'Folder' && !it.hasChildren && it.id == folder2Id}

        when:
        tree = treeApi.folderTree(folder1Id, false)

        then:
        tree
        tree.size() == 4
        tree.find {it.label == 'TreeIntegrationSpec data model' && it.domainType == 'DataModel' && it.hasChildren && it.id == dataModelId}
        tree.find {it.label == 'TreeIntegrationSpec code set' && it.domainType == 'CodeSet' && !it.hasChildren && it.id == codeSetId}
        tree.find {it.label == 'TreeIntegrationSpec terminology' && it.domainType == 'Terminology' && !it.hasChildren && it.id == terminologyId}
        tree.find {it.label == 'TreeIntegrationSpec classification scheme' && it.domainType == 'ClassificationScheme' && !it.hasChildren && it.id == classificationSchemeId}

        when:
        tree = treeApi.itemTree("codeSet",codeSetId, false)

        then:
        tree.size() == 0

        when:
        tree = treeApi.itemTree("dataModel", dataModelId, false)

        then:
        tree
        tree.size() == 1
        tree.find {it.label == 'data class' && it.domainType == 'DataClass' && !it.hasChildren && it.id == dataClassId}
    }

    void 'non-admin user cannot see tree without permissions'() {
        when:
        loginUser()
        List<TreeItem> tree = treeApi.folderTree(null, false)

        then:
        !tree.find {it.label == 'TreeIntegrationSpec root folder'}

        when:
        treeApi.folderTree(rootFolderId, false)

        then:
        HttpClientResponseException exception = thrown()
        exception.status == HttpStatus.FORBIDDEN

        when:
        treeApi.itemTree("dataModel", dataModelId, false)

        then:
        exception = thrown()
        exception.status == HttpStatus.FORBIDDEN
    }

    void 'non-admin user can see the tree when permissions are granted'() {
        given:
        loginAdmin()
        UserGroup readersGroup = userGroupApi.create(new UserGroup (name: 'Readers Group'))
        userGroupId = readersGroup.id
        catalogueUserApi.update(user.id, new CatalogueUser(groups: [readersGroup.id] ))
        securableResourceGroupRoleApi.create("folder", rootFolderId, Role.READER, readersGroup.id)
        logout()

        when:
        loginUser()
        List<TreeItem> tree = treeApi.folderTree(null, false)

        then:
        tree
        tree.size() >= 1
        tree.find {it.label == 'TreeIntegrationSpec root folder' && it.domainType == 'Folder' && it.hasChildren && it.id == rootFolderId}

        when:
        tree = treeApi.folderTree(rootFolderId, false)

        then:
        tree
        tree.size() == 2
        tree.find {it.label == 'TreeIntegrationSpec folder with contents' && it.domainType == 'Folder' && it.hasChildren && it.id == folder1Id}
        tree.find {it.label == 'TreeIntegrationSpec empty folder' && it.domainType == 'Folder' && !it.hasChildren && it.id == folder2Id}

        when:
        tree = treeApi.folderTree(folder1Id, false)

        then:
        tree
        tree.size() == 4
        tree.find {it.label == 'TreeIntegrationSpec data model' && it.domainType == 'DataModel' && it.hasChildren && it.id == dataModelId}
        tree.find {it.label == 'TreeIntegrationSpec code set' && it.domainType == 'CodeSet' && !it.hasChildren && it.id == codeSetId}
        tree.find {it.label == 'TreeIntegrationSpec terminology' && it.domainType == 'Terminology' && !it.hasChildren && it.id == terminologyId}
        tree.find {it.label == 'TreeIntegrationSpec classification scheme' && it.domainType == 'ClassificationScheme' && !it.hasChildren && it.id == classificationSchemeId}

        when:
        tree = treeApi.itemTree("codeSet", codeSetId, false)

        then:
        tree.size() == 0

        when:
        tree = treeApi.itemTree("dataModel", dataModelId, false)

        then:
        tree
        tree.size() == 1
        tree.find {it.label == 'data class' && it.domainType == 'DataClass' && !it.hasChildren && it.id == dataClassId}
    }


    void 'non-admin user cannot see tree when permissions are removed'() {
        given:
        loginAdmin()
        securableResourceGroupRoleApi.delete("folder", rootFolderId, Role.READER, userGroupId)
        logout()

        when:
        loginUser()
        List<TreeItem> tree = treeApi.folderTree(null, false)

        then:
        !tree.find {it.label == 'TreeIntegrationSpec root folder'}

        when:
        treeApi.folderTree(rootFolderId, false)

        then:
        HttpClientResponseException exception = thrown()
        exception.status == HttpStatus.FORBIDDEN

        when:
        treeApi.itemTree("dataModel", dataModelId, false)

        then:
        exception = thrown()
        exception.status == HttpStatus.FORBIDDEN
    }
}
