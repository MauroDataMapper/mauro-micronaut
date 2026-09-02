package org.maurodata.security

import io.micronaut.http.client.exceptions.HttpClientResponseException
import io.micronaut.test.annotation.Sql
import jakarta.inject.Singleton
import org.maurodata.domain.datamodel.DataModel
import org.maurodata.domain.folder.Folder
import org.maurodata.domain.tree.TreeItem
import org.maurodata.persistence.SecuredContainerizedTest
import spock.lang.Shared

@SecuredContainerizedTest
@Singleton
@Sql(scripts = ["classpath:sql/tear-down-datamodel.sql",
    "classpath:sql/tear-down.sql",
    "classpath:sql/tear-down-folder.sql"], phase = Sql.Phase.AFTER_EACH)
class AccessControlServiceSpec extends SecuredIntegrationSpec {

    @Shared
    Folder rootFolder1, rootFolder2, subFolder1, subFolder2, subSubFolder1, subSubFolder2

    DataModel dataModel1, dataModel2

    void setup() {
        loginAdmin()
        rootFolder1 = folderApi.create(new Folder(label: "Root folder 1"))
        rootFolder2 = folderApi.create(new Folder(label: "Root folder 2"))
        subFolder1 = folderApi.create(rootFolder1.id, new Folder(label: "Sub folder 1"))
        subFolder2 = folderApi.create(rootFolder2.id, new Folder(label: "Sub folder 2"))
        subSubFolder1 = folderApi.create(subFolder1.id, new Folder(label: "Sub sub folder 1"))
        subSubFolder2 = folderApi.create(subFolder2.id, new Folder(label: "Sub sub folder 2"))
        dataModel1 = dataModelApi.create(subSubFolder1.id, new DataModel(label: "Data model 1"))
        dataModel2 = dataModelApi.create(subSubFolder2.id, new DataModel(label: "Data model 2"))
        logout()
    }


    void 'No permissions granted'() {
        when: // Not logged in
            logout()
        then:
            seeNothing("User is not authenticated")

        when: // Logged in user
            loginUser()
        then:
            seeNothing("Forbidden")
            logout()

        when: // Logged in admin
            loginAdmin()
        then:
            seeEverything()
            logout()
    }

    void 'Read by everyone on root folder'() {
        when:
            loginAdmin()
            folderApi.allowReadByEveryone(rootFolder1.id)
            logout()

        then: // Not logged in
            seeRootFolder1()
            dontSeeRootFolder2("User is not authenticated")

        then: // Logged in user
            loginUser()
            seeRootFolder1()
            dontSeeRootFolder2("Forbidden")
            logout()

        then: // Logged in admin
            loginAdmin()
            seeEverything()
            logout()
    }


    void 'Read by everyone on sub folder'() {
        when:
            loginAdmin()
            folderApi.allowReadByEveryone(subFolder1.id)
            logout()
        then: // Not logged in
            seeRootFolder1()
            dontSeeRootFolder2("User is not authenticated")

        then: // Logged in user
            loginUser()
            seeRootFolder1()
            dontSeeRootFolder2("Forbidden")
            logout()

        then: // Logged in admin
            loginAdmin()
            seeEverything()
            logout()

    }


    void 'Read by everyone on sub sub folder - Not logged in'() {
        when:
        loginAdmin()
        folderApi.allowReadByEveryone(subSubFolder1.id)
        logout()

        then: // Not logged in
            seeRootFolder1()
            dontSeeRootFolder2("User is not authenticated")

        then: // Logged in user
            loginUser()
            seeRootFolder1()
            dontSeeRootFolder2("Forbidden")
            logout()

        then: // Logged in admin
            loginAdmin()
            seeEverything()
            logout()
    }

    void 'Read by everyone on data model - Not logged in'() {
        when:
            loginAdmin()
            dataModelApi.allowReadByEveryone(dataModel1.id)
            logout()

        then: // Not logged in
            seeRootFolder1()
            dontSeeRootFolder2("User is not authenticated")

        then: // Logged in user
            loginUser()
            seeRootFolder1()
            dontSeeRootFolder2("Forbidden")
            logout()

        then: // Logged in admin
            loginAdmin()
            seeEverything()
            logout()
    }

    void 'Read by authenticated on root folder'() {
        when:
            loginAdmin()
            folderApi.allowReadByAuthenticated(rootFolder1.id)
            logout()

        then: // Not logged in
            seeNothing("User is not authenticated")

        then: // Logged in user
            loginUser()
            seeRootFolder1()
            dontSeeRootFolder2("Forbidden")
            logout()

        then: // Logged in admin
            loginAdmin()
            seeEverything()
            logout()
    }

    void 'Read by authenticated on sub folder'() {
        when:
            loginAdmin()
            folderApi.allowReadByAuthenticated(subFolder1.id)
            logout()

        then: // Not logged in
            seeNothing("User is not authenticated")

        then: // Logged in user
            loginUser()
            seeRootFolder1()
            dontSeeRootFolder2("Forbidden")
            logout()

        then: // Logged in admin
            loginAdmin()
            seeEverything()
            logout()
    }

    void 'Read by authenticated on sub sub folder'() {
        when:
            loginAdmin()
            folderApi.allowReadByAuthenticated(subSubFolder1.id)
            logout()

        then: // Not logged in
            seeNothing("User is not authenticated")

        then: // Logged in user
            loginUser()
            seeRootFolder1()
            dontSeeRootFolder2("Forbidden")
            logout()

        then: // Logged in admin
            loginAdmin()
            seeEverything()
            logout()
    }

    void 'Read by authenticated on data model'() {
        when:
        loginAdmin()
        dataModelApi.allowReadByAuthenticated(dataModel1.id)
        logout()

        then: // Not logged in
            seeNothing("User is not authenticated")

        then: // Logged in user
            loginUser()
            seeRootFolder1()
            dontSeeRootFolder2("Forbidden")
            logout()

        then: // Logged in admin
            loginAdmin()
            seeEverything()
            logout()
    }




    boolean seeRootFolder1() {
        List<TreeItem> treeItems = treeApi.folderTree(null, false)
        treeItems.any {
            it.id == rootFolder1.id
        }

        [rootFolder1, subFolder1, subSubFolder1].each {Folder folder ->
            assert treeApi.folderTree(folder.id, false).size() == 1
            assert folderApi.show(folder.id) != null
        }
        [dataModel1].each {DataModel dataModel ->
            assert treeApi.itemTree("DataModel", dataModel.id, false).size() == 0
            assert dataModelApi.show(dataModel.id) != null
        }
        return true
    }

    boolean seeRootFolder2() {
        List<TreeItem> treeItems = treeApi.folderTree(null, false)
        treeItems.any {
            it.id == rootFolder2.id
        }

        [rootFolder2, subFolder2, subSubFolder2].each {Folder folder ->
            assert treeApi.folderTree(folder.id, false).size() == 1
            assert folderApi.show(folder.id) != null
        }
        [dataModel2].each {DataModel dataModel ->
            assert treeApi.itemTree("DataModel", dataModel.id, false).size() == 0
            assert dataModelApi.show(dataModel.id) != null
        }
        return true
    }



    boolean seeEverything() {
        assert seeRootFolder1()
        assert seeRootFolder2()

        return true
    }

    boolean seeNothing(String message) {
        assert dontSeeRootFolder1(message)
        assert dontSeeRootFolder2(message)
        return true
    }

    boolean dontSeeRootFolder1(String message) {
        List<TreeItem> treeItems = treeApi.folderTree(null, false)
        assert treeItems.find { it.id == rootFolder1.id } == null

        [rootFolder1, subFolder1, subSubFolder1].each {Folder folder ->
            captureException(HttpClientResponseException, message, {
                treeApi.folderTree(folder.id, false)
            })
            captureException(HttpClientResponseException, message, {
                folderApi.show(folder.id)
            })

        }
        [dataModel1].each {DataModel dataModel ->
            captureException(HttpClientResponseException, message, {
                treeApi.itemTree("DataModel", dataModel.id, false)
            })

            captureException(HttpClientResponseException, message, {
                dataModelApi.show(dataModel.id)
            })

        }
        return true
    }
    boolean dontSeeRootFolder2(String message) {
        List<TreeItem> treeItems = treeApi.folderTree(null, false)
        assert treeItems.find { it.id == rootFolder2.id } == null

        [rootFolder2, subFolder2, subSubFolder2].each {Folder folder ->
            captureException(HttpClientResponseException, message, {
                treeApi.folderTree(folder.id, false)
            })
            captureException(HttpClientResponseException, message, {
                folderApi.show(folder.id)
            })

        }
        [dataModel2].each {DataModel dataModel ->
            captureException(HttpClientResponseException, message, {
                treeApi.itemTree("DataModel", dataModel.id, false)
            })

            captureException(HttpClientResponseException, message, {
                dataModelApi.show(dataModel.id)
            })

        }
        return true
    }

    private static void captureException(Class<Exception> clazz, String message, Closure action) {
        try {
            action()
            assert false: "Expected an exception to be thrown"
        } catch (Exception e) {
            assert e.class.isAssignableFrom(clazz)
            assert e.message == message

        }
    }
}
