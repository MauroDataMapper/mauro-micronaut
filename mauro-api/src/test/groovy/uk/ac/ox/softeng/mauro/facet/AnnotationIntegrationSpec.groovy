package uk.ac.ox.softeng.mauro.facet

import io.micronaut.http.HttpStatus
import io.micronaut.http.client.exceptions.HttpClientResponseException
import io.micronaut.runtime.EmbeddedApplication
import io.micronaut.test.annotation.Sql
import jakarta.inject.Inject
import spock.lang.Shared
import spock.lang.Unroll
import uk.ac.ox.softeng.mauro.domain.facet.Annotation
import uk.ac.ox.softeng.mauro.domain.folder.Folder
import uk.ac.ox.softeng.mauro.persistence.ContainerizedTest
import uk.ac.ox.softeng.mauro.testing.CommonDataSpec
import uk.ac.ox.softeng.mauro.web.ListResponse

@ContainerizedTest
@Sql(scripts = "classpath:sql/tear-down-annotation.sql", phase = Sql.Phase.AFTER_EACH)
class AnnotationIntegrationSpec extends CommonDataSpec {

    @Inject
    EmbeddedApplication<? extends EmbeddedApplication> application

    @Shared
    UUID folderId


    void setupSpec() {
        Folder folder = (Folder) POST("$FOLDERS_PATH", folder(), Folder)
        folderId = folder.id
    }

    @Unroll()
    void 'list Annotations should return empty list'() {
        when:
        def response =
                GET("$FOLDERS_PATH/$folderId$ANNOTATION_PATH", ListResponse, Annotation)
        then:
        response.count == 0
    }

    @Unroll
    void 'create annotation'() {
        when:
        Annotation annotation = (Annotation) POST("$FOLDERS_PATH/$folderId$ANNOTATION_PATH",
                iteration, Annotation)

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
        Annotation annotation = (Annotation) POST("$FOLDERS_PATH/$folderId$ANNOTATION_PATH",
                iteration, Annotation)

        when:
        Annotation saved = (Annotation) GET("$FOLDERS_PATH/$folderId$ANNOTATION_PATH/$annotation.id", Annotation)

        then:
        saved
        saved.id == annotation.id

        where:
        iteration << [
                annotationPayload('test label', 'test description')
        ]
    }

    void 'get annotation by Id -should return 404 on invalid domain type/domainId parameters for annotation'() {
        given:
        UUID annotationId = UUID.randomUUID()
        and:
        Annotation annotation = (Annotation) POST("$FOLDERS_PATH/$folderId$ANNOTATION_PATH",
                annotationPayload(), Annotation)
        when:
        (Annotation) GET("$FOLDERS_PATH/$annotationId$ANNOTATION_PATH/$annotation.id", Annotation)

        then:
        HttpClientResponseException exception = thrown()
        exception.status == HttpStatus.NOT_FOUND
    }

    @Unroll
    void 'create child annotation - should create'() {
        given:
        Annotation parent = (Annotation) POST("$FOLDERS_PATH/$folderId$ANNOTATION_PATH",
                annotationPayload('parent test label', 'parent test description'), Annotation)
        and:
        parent
        parent.label == 'parent test label'
        parent.description == 'parent test description'
        !parent.parentAnnotationId

        when:
        Annotation child = (Annotation) POST("$FOLDERS_PATH/$folderId$ANNOTATION_PATH/$parent.id$ANNOTATION_PATH",
                iteration, Annotation)

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
        (Annotation) POST("$FOLDERS_PATH/$folderId$ANNOTATION_PATH/$nonExisting$ANNOTATION_PATH",
                iteration, Annotation)

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
        Annotation annotation = (Annotation) POST("$FOLDERS_PATH/$folderId$ANNOTATION_PATH",
                iteration, Annotation)
        when:
        Annotation retrieved = (Annotation) GET("$FOLDERS_PATH/$folderId$ANNOTATION_PATH/$annotation.id", Annotation)

        then:
        retrieved
        retrieved.id != null
        retrieved.domainType == annotation.class.simpleName
        !retrieved.parentAnnotationId
        retrieved.label == iteration.label
        retrieved.description == iteration.description

        when:
        Annotation child = (Annotation) POST("$FOLDERS_PATH/$folderId$ANNOTATION_PATH/$retrieved.id$ANNOTATION_PATH",
                iteration, Annotation)
        and:
        retrieved = (Annotation) GET("$FOLDERS_PATH/$folderId$ANNOTATION_PATH/$annotation.id", Annotation)

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
        Annotation parent = (Annotation) POST("$FOLDERS_PATH/$folderId$ANNOTATION_PATH",
                annotationPayload(), Annotation)

        Annotation child = (Annotation) POST("$FOLDERS_PATH/$folderId$ANNOTATION_PATH/$parent.id$ANNOTATION_PATH",
                annotationPayload('child label', 'child description'), Annotation)
        when:
        Annotation retrievedParent = (Annotation) GET("$FOLDERS_PATH/$folderId$ANNOTATION_PATH/$parent.id", Annotation)

        Annotation retrieved = (Annotation) GET("$FOLDERS_PATH/$folderId$ANNOTATION_PATH/$parent.id$ANNOTATION_PATH/$parent.id", Annotation)

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
        Annotation parent = (Annotation) POST("$FOLDERS_PATH/$folderId$ANNOTATION_PATH",
                annotationPayload(), Annotation)
        and:
        Annotation child = (Annotation) POST("$FOLDERS_PATH/$folderId$ANNOTATION_PATH/$parent.id$ANNOTATION_PATH",
                [label: 'child label', description: 'child description'], Annotation)

        when:
        ListResponse<Annotation> parentList = (ListResponse<Annotation>) GET("$FOLDERS_PATH/$folderId$ANNOTATION_PATH", ListResponse, Annotation)
        then:
        parentList
        parentList.count == 1
        parentList.items[0].childAnnotations.size() == 1
        parentList.items[0].childAnnotations.id.get(0) == child.id
    }


    void 'get annotation by Id - should return annotation when child '() {
        given:
        Annotation parent = (Annotation) POST("$FOLDERS_PATH/$folderId$ANNOTATION_PATH",
                annotationPayload(), Annotation)
        and:
        Annotation child = (Annotation) POST("$FOLDERS_PATH/$folderId$ANNOTATION_PATH/$parent.id$ANNOTATION_PATH",
                annotationPayload('child label', 'child description'), Annotation)

        when:
        Annotation annotation = (Annotation) GET("$FOLDERS_PATH/$folderId$ANNOTATION_PATH/$child.id", Annotation)

        then:
        annotation
        annotation.id == child.id
        !annotation.childAnnotations
    }

    @Unroll
    void 'get child annotation by Id - should return child annotation'() {
        given:
        Annotation parent = (Annotation) POST("$FOLDERS_PATH/$folderId$ANNOTATION_PATH",
                annotationPayload(), Annotation)
        and:
        Annotation child = (Annotation) POST("$FOLDERS_PATH/$folderId$ANNOTATION_PATH/$parent.id$ANNOTATION_PATH",
                [label: 'child label', description: 'child description'], Annotation)

        when:
        Annotation retrievedChild = (Annotation) GET("$FOLDERS_PATH/$folderId$ANNOTATION_PATH/$parent.id$ANNOTATION_PATH/$child.id", Annotation)

        then:
        retrievedChild
        retrievedChild.id == retrievedChild.id
        retrievedChild.parentAnnotationId == parent.id

        where:
        iteration << [
                annotationPayload('child test label', 'child test description')
        ]
    }

    void 'delete child Annotation - should delete child'() {
        given:
        Annotation parent = (Annotation) POST("$FOLDERS_PATH/$folderId$ANNOTATION_PATH",
                annotationPayload(), Annotation)
        and:
        Annotation child = (Annotation) POST("$FOLDERS_PATH/$folderId$ANNOTATION_PATH/$parent.id$ANNOTATION_PATH",
                annotationPayload('child label', 'child description'), Annotation)
        when:
        HttpStatus status = (HttpStatus) DELETE("$FOLDERS_PATH/$folderId$ANNOTATION_PATH/$parent.id$ANNOTATION_PATH/$child.id", HttpStatus)
        then:
        status == HttpStatus.NO_CONTENT

        when:
        (Annotation) GET("$FOLDERS_PATH/$folderId$ANNOTATION_PATH/$parent.id$ANNOTATION_PATH/$child.id", Annotation)

        then: 'the show endpoint shows the update'
        HttpClientResponseException exception = thrown()
        exception.status == HttpStatus.NOT_FOUND

        when:
        Annotation retrievedParent = (Annotation) GET("$FOLDERS_PATH/$folderId$ANNOTATION_PATH/$parent.id", Annotation)

        then: 'the parent annotation should not show deleted child'
        retrievedParent
        !retrievedParent.childAnnotations
    }


    void 'delete annotation - should delete self and related children'() {
        given:
        Annotation parent = (Annotation) POST("$FOLDERS_PATH/$folderId$ANNOTATION_PATH",
                annotationPayload(), Annotation)
        and:
        Annotation child1 = (Annotation) POST("$FOLDERS_PATH/$folderId$ANNOTATION_PATH/$parent.id$ANNOTATION_PATH",
                annotationPayload('child label 1', 'child description 1 '), Annotation)
        Annotation child2 = (Annotation) POST("$FOLDERS_PATH/$folderId$ANNOTATION_PATH/$parent.id$ANNOTATION_PATH",
                annotationPayload('child label 2 ', 'child description 2'), Annotation)
        when:
        HttpStatus status = (HttpStatus) DELETE("$FOLDERS_PATH/$folderId$ANNOTATION_PATH/$parent.id", HttpStatus)

        then:
        status == HttpStatus.NO_CONTENT

        when:
        (Annotation) GET("$FOLDERS_PATH/$folderId$ANNOTATION_PATH/$parent.id", Annotation)

        then: 'the show endpoint shows the update'
        HttpClientResponseException exception = thrown()
        exception.status == HttpStatus.NOT_FOUND

        when:
        (Annotation) GET("$FOLDERS_PATH/$folderId$ANNOTATION_PATH/$parent.id/annotations/$child1.id", Annotation)

        then: 'Child 1 is not found'
        exception = thrown()
        exception.status == HttpStatus.NOT_FOUND

        when:
        (Annotation) GET("$FOLDERS_PATH/$folderId$ANNOTATION_PATH/$parent.id/annotations/$child2.id", Annotation)

        then: 'Child 2 is not found'
        exception = thrown()
        exception.status == HttpStatus.NOT_FOUND
    }

    void 'delete Annotation - when annotation is child, should still delete'() {
        given:
        Annotation parent = (Annotation) POST("$FOLDERS_PATH/$folderId$ANNOTATION_PATH",
                annotationPayload(), Annotation)
        and:
        Annotation child = (Annotation) POST("$FOLDERS_PATH/$folderId$ANNOTATION_PATH/$parent.id$ANNOTATION_PATH",
                annotationPayload('child label', 'child description'), Annotation)
        when:
        HttpStatus status = (HttpStatus) DELETE("$FOLDERS_PATH/$folderId$ANNOTATION_PATH/$child.id", HttpStatus)

        then:
        status == HttpStatus.NO_CONTENT

        when:
        (Annotation) GET("$FOLDERS_PATH/$folderId$ANNOTATION_PATH/$parent.id$ANNOTATION_PATH/$child.id", Annotation)

        then: 'the show endpoint shows the update'
        HttpClientResponseException exception = thrown()
        exception.status == HttpStatus.NOT_FOUND

        when:
        Annotation retrievedParent = (Annotation) GET("$FOLDERS_PATH/$folderId$ANNOTATION_PATH/$parent.id", Annotation)

        then: 'the parent annotation should not show deleted child'
        retrievedParent
        !retrievedParent.childAnnotations
    }

    void 'delete Parent and child Annotations - when both ids are parent, should throw bad request'() {
        given:
        Annotation parent = (Annotation) POST("$FOLDERS_PATH/$folderId$ANNOTATION_PATH",
                annotationPayload(), Annotation)
        and:
        Annotation child = (Annotation) POST("$FOLDERS_PATH/$folderId$ANNOTATION_PATH/$parent.id$ANNOTATION_PATH",
                annotationPayload('child label', 'child description'), Annotation)
        when:
        (HttpStatus) DELETE("$FOLDERS_PATH/$folderId$ANNOTATION_PATH/$parent.id$ANNOTATION_PATH/$parent.id", HttpStatus)


        then: 'bad request error'
        HttpClientResponseException exception = thrown()
        exception.status == HttpStatus.BAD_REQUEST
    }

}
