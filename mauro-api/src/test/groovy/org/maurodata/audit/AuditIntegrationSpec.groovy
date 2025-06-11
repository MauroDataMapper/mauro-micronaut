package org.maurodata.audit

import org.maurodata.domain.facet.Edit
import org.maurodata.domain.facet.EditType
import org.maurodata.domain.folder.Folder
import org.maurodata.persistence.SecuredContainerizedTest
import org.maurodata.security.SecuredIntegrationSpec
import org.maurodata.web.ListResponse

import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Delete
import io.micronaut.http.annotation.Post
import io.micronaut.http.annotation.Put
import jakarta.inject.Singleton
import org.reflections.Reflections

import java.lang.reflect.Method

@Singleton
@SecuredContainerizedTest
class AuditIntegrationSpec extends SecuredIntegrationSpec {

    void "Test Audit annotation"() {
        when:
        loginAdmin()
        Folder folderResponse = folderApi.create(new Folder(label: 'Test folder'))
        Folder folderResponse2 = folderApi.show(folderResponse.id)
        then:
        folderResponse2.edits.size() == 1
        folderResponse2.edits.first().description == 'Created Folder'
        folderResponse2.edits.first().title == EditType.CREATE

        when:
        ListResponse<Edit> editResponse = editApi.list("folder", folderResponse.id)

        then:
        editResponse.items.size() == 1
        editResponse.items.sort {it.dateCreated}.first().description == 'Created Folder'
        editResponse.items.sort {it.dateCreated}.first().title == EditType.CREATE

        when:
        folderApi.update(folderResponse.id, new Folder(label: "Test folder (updated)"))
        folderResponse2 = folderResponse2 = folderApi.show(folderResponse.id)

        then:
        folderResponse2.edits.size() == 2
        folderResponse2.edits.sort {it.dateCreated}.last().description == 'Updated Folder'
        folderResponse2.edits.sort {it.dateCreated}.last().title == EditType.UPDATE

        when:
        editResponse = editApi.list("folder", folderResponse.id)

        then:
        editResponse.items.size() == 2
        editResponse.items.sort {it.dateCreated}.last().description == 'Updated Folder'
        editResponse.items.sort {it.dateCreated}.last().title == EditType.UPDATE
        logout()
    }

    void "Test Audit annotation on deletion"() {
        when:
        loginAdmin()
        Folder folderResponse = folderApi.create(new Folder(label: 'Test folder'))
        Folder childFolderResponse = folderApi.create(folderResponse.id, new Folder(label: 'Test folder'))
        Folder folderResponse2 = folderApi.show(folderResponse.id)

        then:
        folderResponse2.edits.size() == 1
        folderResponse2.edits.first().description == 'Created Folder'
        folderResponse2.edits.first().title == EditType.CREATE

        when:
        folderApi.delete(folderResponse.id, childFolderResponse.id, new Folder(),true)
        folderResponse2 = folderApi.show(folderResponse.id)

        then:
        folderResponse2.edits.size() == 2
        folderResponse2.edits.find {it.description == 'Created Folder' && it.title == EditType.CREATE }
        folderResponse2.edits.find {it.description == 'Deleted Folder' && it.title == EditType.DELETE }

    }

    void "Test all Post, Put, Delete methods annotated with audit config"() {
        when:
        Reflections reflections = new Reflections("org.maurodata")
        Set<Class> controllerClasses =  reflections.getTypesAnnotatedWith(Controller).
            findAll{!it.name.endsWith("Intercepted")}

        Set<Method> controllerMethods = controllerClasses.collect{controllerClass ->
            controllerClass.declaredMethods.findAll {method ->
                method.declaredAnnotations.any {annotation ->
                    [Post, Put, Delete].contains(annotation.annotationType())
                }
            }

        }.flatten() as Set

        Set<Method> auditedMethods = controllerMethods.findAll{controllerMethod ->
            controllerMethod.declaredAnnotations.any { annotation ->
                annotation.annotationType() == Audit
            }
        }

        Set<Method> unauditedMethods = controllerMethods - auditedMethods

        then:
        unauditedMethods.size() == 0

    }

    void "Test all Delete methods have description or domainType set"() {
        when:
        Reflections reflections = new Reflections("uk.ac.ox.softeng.mauro")
        Set<Class> controllerClasses = reflections.getTypesAnnotatedWith(Controller).
            findAll{!it.name.endsWith("Intercepted")} as Set<Class>

        Set<Method> deleteMethods = controllerClasses.collect{controllerClass ->
            controllerClass.declaredMethods.findAll {method ->
                method.declaredAnnotations.any {annotation ->
                    Delete == annotation.annotationType()
                }
            }
        }.flatten() as Set

        Set<Method> unauditedMethods = deleteMethods.findAll{deleteMethod ->
            deleteMethod.declaredAnnotations.any { annotation ->
                annotation.annotationType() == Audit &&
                ((Audit) annotation).level() != Audit.AuditLevel.FILE_ONLY &&
                    (!((Audit) annotation).description() &&
                        !((Audit) annotation).deletedObjectDomainType())

            }
        }

        then:
        unauditedMethods.size() == 0

    }

}