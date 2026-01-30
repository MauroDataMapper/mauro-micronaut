package org.maurodata.tree

import org.maurodata.domain.classifier.ClassificationScheme
import org.maurodata.domain.datamodel.DataClass
import org.maurodata.domain.datamodel.DataModel
import org.maurodata.domain.folder.Folder
import org.maurodata.domain.model.AdministeredItem
import org.maurodata.domain.security.CatalogueUser
import org.maurodata.domain.security.Role
import org.maurodata.domain.security.UserGroup
import org.maurodata.domain.terminology.CodeSet
import org.maurodata.domain.terminology.Terminology
import org.maurodata.domain.tree.TreeItem
import org.maurodata.persistence.SecuredContainerizedTest
import org.maurodata.security.SecuredIntegrationSpec

import io.micronaut.http.HttpStatus
import io.micronaut.http.client.exceptions.HttpClientResponseException
import jakarta.inject.Singleton
import spock.lang.Shared

@SecuredContainerizedTest
@Singleton
class TreeIntegrationSpec extends SecuredIntegrationSpec {

    @Shared
    Folder rootFolder

    @Shared
    Folder folder1

    @Shared
    Folder folder2

    @Shared
    Folder folder3

    @Shared
    DataModel dataModel1

    @Shared
    DataModel dataModel2

    @Shared
    DataClass dataClass

    @Shared
    CodeSet codeSet

    @Shared
    Terminology terminology

    @Shared
    UUID userGroupId

    @Shared
    ClassificationScheme classificationScheme

    void setupSpec() {
        loginAdmin()
        /***
         * rootFolder
         *   |- folder1
         *        |- folder3
         *             |- dataModel2
         *        |- dataModel1
         *             |- dataClass
         *        |- terminology
         *        |- codeset
         *        |- classification
         *   |- folder2
         *
         *
         *
         *
         */


        rootFolder = folderApi.create(new Folder(label: 'rootFolder'))
        folder1 = folderApi.create(rootFolder.id, new Folder(label: 'folder1'))
        folder2 = folderApi.create(rootFolder.id, new Folder(label: 'folder2'))
        folder3 = folderApi.create(folder1.id, new Folder(label: 'folder3'))

        dataModel1 = dataModelApi.create(folder1.id, new DataModel(label: 'dataModel1'))
        dataClass = dataClassApi.create(dataModel1.id, new DataClass(label: 'dataClass'))

        dataModel2 = dataModelApi.create(folder3.id, new DataModel(label: 'dataModel2'))

        terminology = terminologyApi.create(folder1.id, new Terminology(label: 'terminology'))
        codeSet = codeSetApi.create(folder1.id, new CodeSet(label: 'codeSet'))
        classificationScheme = classificationSchemeApi.create(folder1.id,  new ClassificationScheme(label: 'classificationScheme'))
        logout()
    }

    void 'tree created by admin - viewed by admin'() {
        when: "Log in as admin"
        loginAdmin()

        then: "Admin can see the whole tree"
        canSeeTheWholeTree()
    }

    void 'tree created by admin - viewed by user'() {
        when: "Log in as user"
        loginUser()

        then: "User can't see the tree at all"
        cantSeeAnyTree(HttpStatus.FORBIDDEN)
    }

    void 'tree created by admin - viewed by public'() {
        when: "don't login - public"
        logout()

        then: "public can't see the tree at all"
        cantSeeAnyTree(HttpStatus.UNAUTHORIZED)
    }

    void 'group read permissions are granted'() {
        given:
        loginAdmin()
        UserGroup readersGroup = userGroupApi.create(new UserGroup (name: 'Readers Group'))
        userGroupId = readersGroup.id
        catalogueUserApi.update(user.id, new CatalogueUser(groups: [new UserGroup(id: readersGroup.id)] ))
        securableResourceGroupRoleApi.create("folder", rootFolder.id, Role.READER, readersGroup.id)
        logout()

        when:
        loginAdmin()
        then:
        canSeeTheWholeTree()

        when:
        logout()
        then:
        cantSeeAnyTree(HttpStatus.UNAUTHORIZED)

        when:
        loginUser()
        then:
        canSeeTheWholeTree()

        when: "Then revoke the permissions"
        loginAdmin()
        securableResourceGroupRoleApi.delete("folder", rootFolder.id, Role.READER, userGroupId)

        then:
        canSeeTheWholeTree()

        when:
        logout()

        then: "public can't see the tree at all"
        cantSeeAnyTree(HttpStatus.UNAUTHORIZED)

        when:
        loginUser()
        then: "user can't see the tree at all"
        cantSeeAnyTree(HttpStatus.FORBIDDEN)

    }

    void 'public read permissions are granted'() {
        given:
        loginAdmin()
        folderApi.allowReadByEveryone(rootFolder.id)
        logout()

        when:
        loginAdmin()
        then:
        canSeeTheWholeTree()

        when:
        logout()
        then:
        canSeeTheWholeTree()

        when:
        loginUser()
        then:
        canSeeTheWholeTree()

        when: "Then revoke the permissions"
        loginAdmin()
        folderApi.revokeReadByEveryone(rootFolder.id)
        then:
        canSeeTheWholeTree()

        when:
        logout()
        then:
        cantSeeAnyTree(HttpStatus.UNAUTHORIZED)

        when:
        loginUser()
        then: "public can't see the tree at all"
        cantSeeAnyTree(HttpStatus.FORBIDDEN)

    }

    void 'authorized read permissions are granted'() {
        given:
        loginAdmin()
        folderApi.allowReadByAuthenticated(rootFolder.id)
        logout()

        when:
        loginAdmin()
        then:
        canSeeTheWholeTree()

        when:
        logout()
        then:
        cantSeeAnyTree(HttpStatus.UNAUTHORIZED)

        when:
        loginUser()
        then:
        canSeeTheWholeTree()

        when: "Then revoke the permissions"
        loginAdmin()
        folderApi.revokeReadByAuthenticated(rootFolder.id)
        then:
        canSeeTheWholeTree()

        when:
        logout()
        then:
        cantSeeAnyTree(HttpStatus.UNAUTHORIZED)

        when:
        loginUser()
        then: "user can't see the tree at all"
        cantSeeAnyTree(HttpStatus.FORBIDDEN)

    }

    private treeContains(AdministeredItem parentItem, boolean foldersOnly, Map<AdministeredItem, Boolean> childItems) {
        List<TreeItem> tree = []
        if(parentItem == null || parentItem.domainType == "Folder" || parentItem.domainType == "VersionedFolder") {
            tree = treeApi.folderTree(parentItem?.id, foldersOnly)
        } else {
            tree = treeApi.itemTree(parentItem.domainType, parentItem.id, foldersOnly)
        }

        assert tree != null
        assert tree.size() == childItems.size()
        assert childItems.every {childItem, hasChildren ->
            tree.find {treeItem ->
                treeItem.label == childItem.label &&
                    treeItem.domainType == childItem.domainType &&
                    treeItem.hasChildren == hasChildren &&
                    treeItem.id == childItem.id
            }
        }
        return true
    }

    private boolean canSeeTheWholeTree() {
        assert treeContains(null, false, [(rootFolder): true])
        assert treeContains(rootFolder, false, [(folder1): true, (folder2): false])
        assert treeContains(folder1, false, [(folder3): true, (dataModel1): true, (codeSet): false, (terminology): false, (classificationScheme): false])
        assert treeContains(codeSet, false, [:])
        assert treeContains(classificationScheme, false, [:])
        assert treeContains(terminology, false, [:])
        assert treeContains(dataModel1, false, [(dataClass): false])
        assert treeContains(folder3, false, [(dataModel2): false])
        return true
    }

    private boolean cantSeeAnyTree(HttpStatus expectedStatus) {
        assert treeContains(null, false, [:])

        [rootFolder, folder1, folder2, folder3, dataModel1, dataModel2, dataClass, terminology, classificationScheme, codeSet].
            each {treeItem ->
            try {
                treeContains(treeItem, false, [:])
                assert false : "Exception should have been thrown"
            } catch (HttpClientResponseException e){
                assert e.status == expectedStatus
            }
        }
        return true
    }
}
