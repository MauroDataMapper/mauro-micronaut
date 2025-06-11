package org.maurodata.facet

import org.maurodata.api.facet.AnnotationApi
import org.maurodata.api.folder.FolderApi

import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpStatus
import io.micronaut.http.client.exceptions.HttpClientResponseException
import io.micronaut.runtime.EmbeddedApplication
import io.micronaut.test.annotation.Sql
import jakarta.inject.Inject
import jakarta.inject.Singleton
import spock.lang.Shared
import spock.lang.Unroll
import org.maurodata.domain.facet.Annotation
import org.maurodata.domain.folder.Folder
import org.maurodata.persistence.ContainerizedTest
import org.maurodata.testing.CommonDataSpec
import org.maurodata.web.ListResponse

@ContainerizedTest
@Singleton
@Sql(scripts = "classpath:sql/tear-down-annotation.sql", phase = Sql.Phase.AFTER_EACH)
class AnnotationIntegrationSpec extends CommonDataSpec {

    @Shared
    UUID folderId


    void setup() {
        Folder folder = folderApi.create(folder())
        folderId = folder.id
    }

    @Unroll()
    void 'list Annotations should return empty list'() {
        when:
        ListResponse<Annotation> response = annotationApi.list("folder", folderId)
        then:
        response.count == 0
    }

    @Unroll
    void 'create annotation'() {
        when:
        Annotation annotation = annotationApi.create("folder", folderId, iteration)

        then:
        annotation
        annotation.id != null
        annotation.domainType == "Annotation"
        !annotation.parentAnnotationId
        annotation.label == iteration.label
        annotation.description == iteration.description

        where:
        iteration << [
                annotationPayload()
        ]
    }

    void 'get annotation by Id -should return annotation'() {
        given:
        Annotation annotation = annotationApi.create("folder", folderId, annotationPayload('test label', 'test description'))

        when:
        Annotation saved = annotationApi.show("folder", folderId, annotation.id)

        then:
        saved
        saved.id == annotation.id

    }

    void 'get annotation by Id -should return 404 on invalid domain type/domainId parameters for annotation'() {
        given:
        UUID incorrectFolderId = UUID.randomUUID()
        and:
        Annotation annotation = annotationApi.create("folder", folderId, annotationPayload())
        when:
        annotation = annotationApi.show("folder", incorrectFolderId, annotation.id)

        then:
        !annotation
    }

    @Unroll
    void 'create child annotation - should create'() {
        given:
        Annotation parent = annotationApi.create("folder", folderId, annotationPayload('parent test label', 'parent test description'))
        and:
        parent
        parent.label == 'parent test label'
        parent.description == 'parent test description'
        !parent.parentAnnotationId

        when:
        Annotation child = annotationApi.create("folder", folderId, parent.id, iteration)

        then:
        child
        child.label == iteration.label
        child.description == iteration.description
        child.parentAnnotationId == parent.id
        child.multiFacetAwareItemId == parent.multiFacetAwareItemId
        child.multiFacetAwareItemDomainType == parent.multiFacetAwareItemDomainType

        where:
        iteration << [
                annotationPayload('child test label', 'child test description')
        ]
    }

    @Unroll
    void 'create child annotation -parent does not exist -should throw exception'() {
        given:
        UUID nonExisting = UUID.fromString('0f14d0ab-9605-4a62-a9e4-5ed26688389b')
        when:
        annotationApi.create("folder", folderId, nonExisting, iteration)

        then:
        HttpClientResponseException exception = thrown()
        exception.status == HttpStatus.BAD_REQUEST

        where:
        iteration << [
                annotationPayload('child test label', 'child test description')
        ]
    }

    @Unroll
    void 'get annotation by Id should show nested where appropriate'() {
        given:
        Annotation annotation = annotationApi.create("folder", folderId, iteration)
        when:
        Annotation retrieved = annotationApi.show("folder", folderId, annotation.id)

        then:
        retrieved
        retrieved.id != null
        retrieved.domainType == annotation.class.simpleName
        !retrieved.parentAnnotationId
        retrieved.label == iteration.label
        retrieved.description == iteration.description

        when:
        Annotation child = annotationApi.create("folder", folderId, retrieved.id, iteration)
        and:
        retrieved = annotationApi.show("folder", folderId, annotation.id)

        then:
        retrieved
        retrieved.id != null
        retrieved.domainType == annotation.class.simpleName
        !retrieved.parentAnnotationId
        retrieved.label == iteration.label
        retrieved.description == iteration.description
        retrieved.childAnnotations.size() == 1
        retrieved.childAnnotations.id.first() == child.id

        where:
        iteration << [
                annotationPayload()
        ]

    }

    void 'get child annotation - child is parent not child  -should return parent'() {
        given:
        Annotation parent = annotationApi.create("folder", folderId, annotationPayload())

        Annotation child = annotationApi.create("folder", folderId, parent.id, annotationPayload('child label', 'child description'))
        when:
        Annotation retrievedParent = annotationApi.show("folder", folderId, parent.id)

        Annotation retrieved = annotationApi.getChildAnnotation("folder", folderId, parent.id, parent.id)

        then:
        retrievedParent
        retrievedParent.childAnnotations

        retrieved
        retrieved.id == parent.id
        retrieved.childAnnotations.size() == 1
        retrieved.childAnnotations.first().id == child.id
    }

    void 'List annotations - should show nesting'() {
        given:
        Annotation parent = annotationApi.create("folder",folderId, annotationPayload())
        and:
        Annotation child = annotationApi.create("folder", folderId, parent.id,
                new Annotation(label: 'child label', description: 'child description'))

        when:
        ListResponse<Annotation> parentList = annotationApi.list("folder", folderId)
        then:
        parentList
        parentList.count == 1
        parentList.items[0].childAnnotations.size() == 1
        parentList.items[0].childAnnotations.id.get(0) == child.id
    }


    void 'get annotation by Id - should return annotation when child '() {
        given:
        Annotation parent = annotationApi.create("folder", folderId, annotationPayload())
        and:
        Annotation child = annotationApi.create("folder", folderId, parent.id, annotationPayload('child label', 'child description'))

        when:
        Annotation annotation = annotationApi.show("folder", folderId, child.id)

        then:
        annotation
        annotation.id == child.id
        !annotation.childAnnotations
    }

    @Unroll
    void 'get child annotation by Id - should return child annotation'() {
        given:
        Annotation parent = annotationApi.create("folder", folderId, annotationPayload())
        and:
        Annotation child = annotationApi.create("folder", folderId, parent.id, new Annotation(label: 'child label', description: 'child description'))

        when:
        Annotation retrievedChild = annotationApi.getChildAnnotation("folder", folderId, parent.id, child.id)

        then:
        retrievedChild
        retrievedChild.id == retrievedChild.id
        retrievedChild.parentAnnotationId == parent.id

    }

    void 'delete child Annotation - should delete child'() {
        given:
        Annotation parent = annotationApi.create("folder", folderId, annotationPayload())
        and:
        Annotation child = annotationApi.create("folder", folderId, parent.id, annotationPayload('child label', 'child description'))
        when:
        HttpResponse response = annotationApi.delete("folder", folderId, parent.id, child.id)
        then:
        response.status == HttpStatus.NO_CONTENT

        when:
        Annotation annotation = annotationApi.getChildAnnotation("folder", folderId, parent.id, child.id)

        then: 'the show endpoint shows the update'
        !annotation

        when:
        Annotation retrievedParent = annotationApi.show("folder", folderId, parent.id)

        then: 'the parent annotation should not show deleted child'
        retrievedParent
        !retrievedParent.childAnnotations
    }


    void 'delete annotation - should delete self and related children'() {
        given:
        Annotation parent = annotationApi.create("folder", folderId, annotationPayload())
        and:
        Annotation child1 = annotationApi.create("folder", folderId, parent.id, annotationPayload('child label 1', 'child description 1 '))
        Annotation child2 = annotationApi.create("folder", folderId, parent.id, annotationPayload('child label 2 ', 'child description 2'))
        when:
        HttpResponse response = annotationApi.delete("folder", folderId, parent.id)

        then:
        response.status == HttpStatus.NO_CONTENT

        when:
        Annotation annotation = annotationApi.show("folder", folderId, parent.id)

        then: 'the show endpoint shows the update'
        !annotation

        when:
        annotation = annotationApi.getChildAnnotation("folder", folderId, parent.id, child1.id)

        then: 'Child 1 is not found'
        !annotation

        when:
        annotation = annotationApi.getChildAnnotation("folder", folderId, parent.id, child2.id)

        then: 'Child 2 is not found'
        !annotation
    }

    void 'delete Annotation - when annotation is child, should still delete'() {
        given:
        Annotation parent = annotationApi.create("folder", folderId, annotationPayload())
        and:
        Annotation child = annotationApi.create("folder", folderId, parent.id, annotationPayload('child label', 'child description'))
        when:
        HttpResponse response = annotationApi.delete("folder", folderId, child.id)

        then:
        response.status == HttpStatus.NO_CONTENT

        when:
        Annotation annotation = annotationApi.getChildAnnotation("folder", folderId, parent.id, child.id)

        then: 'the show endpoint shows the update'
        !annotation

        when:
        Annotation retrievedParent = annotationApi.show("folder", folderId, parent.id)

        then: 'the parent annotation should not show deleted child'
        retrievedParent
        !retrievedParent.childAnnotations
    }

    void 'delete Parent and child Annotations - when both ids are parent, should throw bad request'() {
        given:
        Annotation parent = annotationApi.create("folder", folderId, annotationPayload())
        and:
        Annotation child = annotationApi.create("folder", folderId, parent.id, annotationPayload('child label', 'child description'))
        when:
        annotationApi.delete("folder", folderId, parent.id, parent.id)

        then: 'bad request error'
        HttpClientResponseException exception = thrown()
        exception.status == HttpStatus.BAD_REQUEST
    }

}
