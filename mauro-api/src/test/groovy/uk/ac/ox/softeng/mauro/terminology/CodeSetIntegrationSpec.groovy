package uk.ac.ox.softeng.mauro.terminology


import io.micronaut.http.uri.UriBuilder
import io.micronaut.runtime.EmbeddedApplication
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import jakarta.inject.Inject
import spock.lang.Shared
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
    UUID termId

    @Shared
    UUID codeSetId

    @Shared
    def terminologyPayload

    @Shared
    def codeSetPayload

    @Shared
    def termResponse

    def setupSpec() {
        def folderPayload = folder()
        terminologyPayload = terminology()
        codeSetPayload = codeSet()

        def folderResponse = POST('/folders', folderPayload)
        folderId = UUID.fromString(folderResponse.id as String)

        def terminologyResponse = POST("/folders/$folderId/terminologies", terminologyPayload)
        terminologyId = UUID.fromString(terminologyResponse.id as String)
        println("folderId: $folderId, terminologyId: $terminologyId")

        termResponse = POST("/terminologies/$terminologyId/terms", termPayload())
        termId = UUID.fromString(termResponse.id)
    }

    def cleanupSpec() {
        application.stop()
    }


    void 'test codeSet post'() {
        when:
        def response = POST("/folders/$folderId/codeSets", codeSetPayload)
        codeSetId = UUID.fromString(response.id as String)

        then:
        response
        response.label == "Test code set"
        response.path == 'co:Test code set$main'
        response.description == "code set description"
        response.author == "A.N. Other"
        response.organisation == "uk.ac.gridpp.ral.org"
        response.terms == null
    }


    void 'test codeSet getById'() {
        given:
        def response = POST("/folders/$folderId/codeSets", codeSetPayload)
        codeSetId = UUID.fromString(response.id as String)
        when:
        def putResponse = GET("/codeSets/$codeSetId")
        println("****** new code setId: $codeSetId")
        then:
        putResponse != null
    }

    void 'test codeSet update CodeSet'() {
        given:
        def response = POST("/folders/$folderId/codeSets", codeSetPayload)
        codeSetId = UUID.fromString(response.id as String)

        def updatedCodeSet = [
                label       : response.label,
                description : response.description,
                author      : 'Updated new author',
                organisation: response.organisation
        ]

        when:
        def putResponse = PUT("/codeSets/$codeSetId", updatedCodeSet)

        then:
        putResponse != null
    }

    void 'test codeSet add Term to CodeSet'() {
        given:
        def response = POST("/folders/$folderId/codeSets", codeSetPayload)
        codeSetId = UUID.fromString(response.id as String)

        def updatedCodeSet = [
                label       : response.label,
                description : response.description,
                author      : response.author,
                organisation: response.organisation
        ]
        when:
        URI uri = UriBuilder.of("/codeSets/$codeSetId")
                .path("/terms/$termId")
                .build();
        def putResponse = PUT(uri, updatedCodeSet)

        then:
        putResponse != null
    }


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
        [label: 'Test terminology']
    }

    def termPayload() {
        [code: 'B15.0', definition: 'Hepatitis A with hepatic coma']
    }
}