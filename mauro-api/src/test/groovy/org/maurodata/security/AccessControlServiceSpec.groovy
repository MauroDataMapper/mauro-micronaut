package org.maurodata.security

import io.micronaut.http.client.exceptions.HttpClientResponseException
import io.micronaut.test.annotation.Sql
import jakarta.inject.Singleton
import org.maurodata.domain.datamodel.DataModel
import org.maurodata.domain.folder.Folder
import org.maurodata.domain.model.Model
import org.maurodata.domain.security.CatalogueUser
import org.maurodata.domain.security.Role
import org.maurodata.domain.security.UserGroup
import org.maurodata.domain.terminology.CodeSet
import org.maurodata.domain.terminology.Terminology
import org.maurodata.domain.tree.TreeItem
import org.maurodata.persistence.SecuredContainerizedTest

@SecuredContainerizedTest
@Singleton
@Sql(scripts = ["classpath:sql/tear-down-datamodel.sql",
    "classpath:sql/tear-down.sql",
    "classpath:sql/tear-down-folder.sql",
    /* "classpath:sql/tear-down-user-group.sql" */], phase = Sql.Phase.AFTER_EACH)
class AccessControlServiceSpec extends SecuredIntegrationSpec {


    Map<List<Folder>, List<Model>> componentsByPath = [:]

    void setup() {
        loginAdmin()
        addRootFolders(["Root folder 1", "Root folder 2"])
        addSubFolders()
        addSubSubFolders()
        addModelsToAllPaths()
        logout()
    }




    void 'No permissions granted'() {
        when: // Not logged in
            logout()
        then:
            cantSeeAnything("User is not authenticated")

        when: // Logged in user
            loginUser()
        then: // Can't see anything
            cantSeeAnything("Forbidden")

        when: // Logged in admin
            loginAdmin()
        then: // Can see everything
            canSeeEverything()
            logout()
    }

    void 'Read by everyone on root folder granted'() {
        when:
            loginAdmin()
            Folder rootFolder1 = componentsByPath.keySet().find {it.size() == 1}.first()
            folderApi.allowReadByEveryone(rootFolder1.id)
            logout()

        and: // Not logged in
            logout()
        then:
            canSeeEverythingBelowFolder([rootFolder1], "User is not authenticated")

        when: // Logged in
            loginUser()
        then:
            canSeeEverythingBelowFolder([rootFolder1], "Forbidden")

        when: // Logged in admin
            loginAdmin()
        then: // Can see everything
            canSeeEverything()
            logout()
    }

    void 'Read by authenticated users on root folder granted'() {
        when:
            loginAdmin()
            Folder rootFolder1 = componentsByPath.keySet().find {it.size() == 1}.first()
            folderApi.allowReadByAuthenticated(rootFolder1.id)
            logout()

        and: // Not logged in
            logout()
        then:
            cantSeeAnything("User is not authenticated")

        when: // Logged in
            loginUser()
        then:
            canSeeEverythingBelowFolder([rootFolder1], "Forbidden")

        when: // Logged in admin
            loginAdmin()
        then: // Can see everything
            canSeeEverything()
            logout()
    }

    void 'Read permissions to user group on root folder granted'() {
        when:
            loginAdmin()
            Folder rootFolder1 = componentsByPath.keySet().find {it.size() == 1}.first()
            UserGroup testUserGroup = userGroupApi.create(new UserGroup(name: "Test User Group"))
            catalogueUserApi.update(user.id, new CatalogueUser(groups: [testUserGroup]))
            securableResourceGroupRoleApi.create("folder", rootFolder1.id, Role.READER, testUserGroup.id)
            logout()

        and: // Not logged in
            logout()
            then:
            cantSeeAnything("User is not authenticated")

        when: // Logged in
            loginUser()
            then:
            canSeeEverythingBelowFolder([rootFolder1], "Forbidden")

        when: // Logged in admin
            loginAdmin()
        then: // Can see everything
            canSeeEverything()
            logout()

        cleanup:
            loginAdmin()
            userGroupApi.delete(testUserGroup.id, testUserGroup)
            logout()

    }


    void 'Read by everyone on sub folder granted'() {
        when:
            loginAdmin()
            List<Folder> subFolderPath1 = componentsByPath.keySet().find {it.size() == 2}
            Folder subFolder1 = subFolderPath1.last()
            folderApi.allowReadByEveryone(subFolder1.id)
            logout()

        and: // Not logged in
            logout()
        then:
            canSeeEverythingBelowFolder(subFolderPath1, "User is not authenticated")

        when: // Logged in
            loginUser()
        then:
            canSeeEverythingBelowFolder(subFolderPath1, "Forbidden")

        when: // Logged in admin
            loginAdmin()
        then: // Can see everything
            canSeeEverything()
            logout()
    }

    void 'Read by authenticated users on sub folder granted'() {
        when:
            loginAdmin()
            List<Folder> subFolderPath1 = componentsByPath.keySet().find {it.size() == 2}
            Folder subFolder1 = subFolderPath1.last()
            folderApi.allowReadByAuthenticated(subFolder1.id)
            logout()

        and: // Not logged in
            logout()
        then:
            cantSeeAnything("User is not authenticated")

        when: // Logged in
            loginUser()
        then:
            canSeeEverythingBelowFolder(subFolderPath1, "Forbidden")

        when: // Logged in admin
            loginAdmin()
        then: // Can see everything
            canSeeEverything()
            logout()
    }

    void 'Read permissions to user group on sub folder granted'() {
        when:
            loginAdmin()
            List<Folder> subFolderPath1 = componentsByPath.keySet().find {it.size() == 2}
            Folder subFolder1 = subFolderPath1.last()
            UserGroup testUserGroup = userGroupApi.create(new UserGroup(name: "Test User Group"))
            catalogueUserApi.update(user.id, new CatalogueUser(groups: [testUserGroup]))
            securableResourceGroupRoleApi.create("folder", subFolder1.id, Role.READER, testUserGroup.id)
            logout()

        and: // Not logged in
            logout()
        then:
            cantSeeAnything("User is not authenticated")

        when: // Logged in
            loginUser()
        then:
            canSeeEverythingBelowFolder(subFolderPath1, "Forbidden")

        when: // Logged in admin
            loginAdmin()
        then: // Can see everything
            canSeeEverything()
            logout()

        cleanup:
            loginAdmin()
            userGroupApi.delete(testUserGroup.id, testUserGroup)
            logout()

    }


    void addModelsToAllPaths() {
        componentsByPath.keySet().each {path ->
            addModelAtPath(path, new DataModel(label: path.last().label + " data model"))
            addModelAtPath(path, new Terminology(label: path.last().label + " terminology"))
            addModelAtPath(path, new CodeSet(label: path.last().label + " codeset"))
        }
    }

    void addRootFolders(List<String> folderLabels) {
        folderLabels.each { label ->
            addRootFolder(new Folder(label: label))
        }
    }

    void addSubFolders() {
        List<List<Folder>> allPaths = []
        allPaths.addAll(componentsByPath.keySet())
        allPaths.each { path ->
            addModelAtPath(path, new Folder(label: path.last().label + " child"))
        }
    }

    void addSubSubFolders() {
        List<List<Folder>> allPaths = []
        allPaths.addAll(componentsByPath.keySet())
        allPaths.each { path ->
            if(path.size() > 1) {
                addModelAtPath(path, new Folder(label: path.last().label + " child"))
            }
        }
    }

    Folder addRootFolder(Folder folder) {
        Folder f = folderApi.create(folder)
        componentsByPath[[f]] = []
        return f
    }


    Model addModelAtPath(List<Folder> folders, Model model) {
        Model m = null
        if(model instanceof Folder) {
            m = folderApi.create(folders.last().id, model)
            componentsByPath[folders + [m]] = []
        } else {
            if (model instanceof DataModel) {
                m = dataModelApi.create(folders.last().id, model)
            }
            if (model instanceof Terminology) {
                m = terminologyApi.create(folders.last().id, model)
            }
            if (model instanceof CodeSet) {
                m = codeSetApi.create(folders.last().id, model)
            }
            componentsByPath[folders].add(m)
        }
        return m
    }



    boolean cantSeeModel(List<Folder> path, Model model, String message) {

        assert folderTreeDoesNotContain(model.id, path.size() > 0 ? path.last()?.id : null)

        if(model instanceof Folder) {
            captureException(HttpClientResponseException, message, {
                treeApi.folderTree(model.id, false)
            })
            captureException(HttpClientResponseException, message, {
                folderApi.show(model.id)
            })
        }
        if(model instanceof DataModel) {
            captureException(HttpClientResponseException, message, {
                treeApi.itemTree('DataModel', model.id, false)
            })

            captureException(HttpClientResponseException, message, {
                dataModelApi.show(model.id)
            })

        }
        if(model instanceof Terminology) {
            captureException(HttpClientResponseException, message, {
                treeApi.itemTree('Terminology', model.id, false)
            })

            captureException(HttpClientResponseException, message, {
                terminologyApi.show(model.id)
            })

        }
        if(model instanceof CodeSet) {
            captureException(HttpClientResponseException, message, {
                treeApi.itemTree('CodeSet', model.id, false)
            })

            captureException(HttpClientResponseException, message, {
                codeSetApi.show(model.id)
            })

        }
        return true

    }

    boolean canSeeModel(List<Folder> path, Model model) {

        assert folderTreeContains(model.id, path.size() > 0 ? path.last()?.id : null)

        if(model instanceof Folder) {
            treeApi.folderTree(model.id, false)
            folderApi.show(model.id)
        }
        if(model instanceof DataModel) {
            treeApi.itemTree('DataModel', model.id, false)
            dataModelApi.show(model.id)
        }
        if(model instanceof Terminology) {
            treeApi.itemTree('Terminology', model.id, false)
            terminologyApi.show(model.id)
        }
        if(model instanceof CodeSet) {
            treeApi.itemTree('CodeSet', model.id, false)
            codeSetApi.show(model.id)
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

    private boolean folderTreeDoesNotContain(UUID itemId, UUID parentFolderId) {
        try {
            List<TreeItem> treeItems =  treeApi.folderTree(parentFolderId, false)
            return !treeItems.any { it.id == itemId }
        } catch (Exception e) {
            return true
        }
    }
    private boolean folderTreeContains(UUID itemId, UUID parentFolderId) {
        try {
            List<TreeItem> treeItems =  treeApi.folderTree(parentFolderId, false)
            return treeItems.any { it.id == itemId }
        } catch (Exception e) {
            return false
        }
    }

    void cantSeeAnything(String message) {
        componentsByPath.each {path, models ->
            assert cantSeeModel(path.take(path.size() - 1), path.last(), message)

            models.each {model ->
                assert cantSeeModel(path, model, message)
            }
        }
    }

    void canSeeEverything() {
        componentsByPath.each {path, models ->
            canSeeModel(path.take(path.size() - 1), path.last())
            models.each {model ->
                canSeeModel(path, model)
            }
        }
    }

    void canSeeEverythingBelowFolder(List<Folder> folders, message) {
        componentsByPath.each {path, models ->
            // If we're below in the tree, then we can see the folder, and all the models within it
            if(isPrefix(folders.id, path.id)) {
                assert canSeeModel(path.take(path.size() - 1), path.last())
                models.each {model ->
                    assert canSeeModel(path, model)
                }
            }
            // If we're above in the tree, then we can see the folder, but none of the models within it
            else if(isPrefix(path.id, folders.id)) {
                assert canSeeModel(path.take(path.size() - 1), path.last())
                models.each {model ->
                    assert cantSeeModel(path, model, message)
                }
            }
            // Otherwise we can't see the folder, or the models within
            else {
                assert cantSeeModel(path.take(path.size() - 1), path.last(), message)
                models.each {model ->
                    assert cantSeeModel(path, model, message)
                }
            }

        }
    }

    static <T> boolean isPrefix(List<T> prefix, List<T> list) {
        return prefix.size() <= list.size()
            && list.subList(0, prefix.size()).equals(prefix);
    }

}
