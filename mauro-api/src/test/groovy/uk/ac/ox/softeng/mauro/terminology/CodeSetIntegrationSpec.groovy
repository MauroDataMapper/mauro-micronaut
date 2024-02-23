package uk.ac.ox.softeng.mauro.terminology

import io.micronaut.runtime.EmbeddedApplication
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import jakarta.inject.Inject
import spock.lang.Shared
import uk.ac.ox.softeng.mauro.domain.facet.Metadata
import uk.ac.ox.softeng.mauro.testing.BaseIntegrationSpec

@MicronautTest
class CodeSetIntegrationSpec extends BaseIntegrationSpec {

    @Inject
    @Shared
    EmbeddedApplication<?> application

    @Shared
    UUID folderId

    @Shared
    UUID terminologyId

    @Shared
    def terminologyPayload
    @Shared
    def termPayload

    @Shared
    def codeSetPayload

    def setupSpec() {
        def folderPayload = folder()
        terminologyPayload = terminology()
        codeSetPayload = codeSet()

        def folderResponse = POST('/folders', folderPayload)
        folderId = UUID.fromString(folderResponse.id)

        def terminologyResponse = POST("/folders/$folderId/terminologies", terminologyPayload)
        terminologyId = UUID.fromString(terminologyResponse.id)
        println("folderId: $folderId, terminologyId: $terminologyId")
        termPayload = termPayload()
    }

    def cleanupSpec(){
        application.stop()
    }


    void 'test codeSet post'() {
        given:
        folderId

        and:

        def termResponse = POST("/terminologies/$terminologyId/terms", termPayload)
        def termId = UUID.fromString(termResponse.id)
        when:
        def response = POST("/folders/$folderId/codeSets", codeSetPayload)

        then:
        response
        response.label == "Test code set"
        response.path == 'co:Test code set$main'
        response.description == "code set description"
        response.author == "A.N. Other"
        response.organisation == "uk.ac.gridpp.ral.org"
        response.terms.properties.size() == 1
    }

//    void 'test codeSet delete'() {
//        given:
//        folderResponse
//        folderId = UUID.fromString(folderResponse.id)
//
//        when:
//        response = POST("/folders/$folderId/codeSets",
//                [
//                        label       : "Test code set",
//                        description : "code set description",
//                        author      : "A.N. Other",
//                        organisation: "uk.ac.gridpp.ral.org"])
//
//        then:
//        response
//        response.label == "Test code set"
//        response.description == "code set description"
//        response.author == "A.N. Other"
//        response.organisation == "uk.ac.gridpp.ral.org"
//    }

    def codeSet() {
        [
                label       : "Test code set",
                description : "code set description",
                author      : "A.N. Other",
                organisation: "uk.ac.gridpp.ral.org"
]
    }

    def folder() {
        [label: 'Test folder']
    }


    def terminology() {
        [  label: 'Test terminology']
    }

    def termPayload() {
        [code:  'B15.0', definition:  'Hepatitis A with hepatic coma']
    }
}