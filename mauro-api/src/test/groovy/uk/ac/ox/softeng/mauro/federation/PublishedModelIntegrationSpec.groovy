package uk.ac.ox.softeng.mauro.federation

import uk.ac.ox.softeng.mauro.domain.datamodel.DataClass
import uk.ac.ox.softeng.mauro.domain.datamodel.DataModel
import uk.ac.ox.softeng.mauro.domain.federation.PublishedModelResponse
import uk.ac.ox.softeng.mauro.domain.folder.Folder
import uk.ac.ox.softeng.mauro.domain.terminology.CodeSet
import uk.ac.ox.softeng.mauro.domain.terminology.Terminology
import uk.ac.ox.softeng.mauro.persistence.SecuredContainerizedTest
import uk.ac.ox.softeng.mauro.security.SecuredIntegrationSpec

import io.micronaut.runtime.EmbeddedApplication
import jakarta.inject.Inject
import spock.lang.Shared

@SecuredContainerizedTest
class PublishedModelIntegrationSpec extends SecuredIntegrationSpec {

    @Inject
    EmbeddedApplication<?> application

    @Shared
    UUID folderId

    @Shared
    UUID dataModelId

    @Shared
    UUID terminologyId
    @Shared
    UUID codeSetId

    void setupSpec() {
        loginAdmin()
        folderId = ((Folder) POST("$FOLDERS_PATH", folder(), Folder)).id
        dataModelId = ((DataModel) POST("$FOLDERS_PATH/$folderId$DATAMODELS_PATH", [label: 'PublishedModelIntegrationSpec data model'], DataModel)).id
        ((DataClass) POST("$DATAMODELS_PATH/$dataModelId$DATACLASSES_PATH", [label: 'PublishedModelIntegrationSpec data class'], DataClass)).id

        terminologyId = ((Terminology) POST("$FOLDERS_PATH/$folderId$TERMINOLOGIES_PATH", [label: 'PublishedModelIntegrationSpecc terminology'], Terminology)).id
        codeSetId = ((CodeSet) POST("$FOLDERS_PATH/$folderId$CODE_SET_PATH", [label: 'PublishedModelIntegrationcode set'], CodeSet)).id
        (DataModel) PUT("$DATAMODELS_PATH/$dataModelId/finalise", [versionChangeType: 'major',
                                                                                                  versionTag       : 'versionTagString'], DataModel)
        (Terminology) PUT("$TERMINOLOGIES_PATH/$terminologyId/finalise", [versionChangeType: 'major',
                                                                                                             versionTag       : 'versionTagString'], Terminology)
        (CodeSet) PUT("$CODE_SET_PATH/$codeSetId/finalise", [versionChangeType: 'major',
                                                                                        versionTag       : 'versionTagString'], CodeSet)

        logout()
    }

    void 'admin user -get published models - should return published models'() {
        given:
        loginAdmin()

        List<String> sortedFinalisedModelIds = List.of(codeSetId.toString(), dataModelId.toString(), terminologyId.toString())
        .toSorted()


        when:
        PublishedModelResponse publishedModelResponse = (PublishedModelResponse) GET(PUBLISHED_MODELS_PATH, PublishedModelResponse)

        then:
        publishedModelResponse
        publishedModelResponse.publishedModels.size() == 3
        List<String> publishedModelIds = publishedModelResponse.publishedModels.collect{it.modelId}.toSorted()
        publishedModelIds == sortedFinalisedModelIds
        List<String> linksContentType = publishedModelResponse.publishedModels.collectMany{ it.links.collect {it.contentType}}.toSorted()
        linksContentType.size() == 3
        linksContentType == List.of( 'application/mauro.codeset+json','application/mauro.datamodel+json','application/mauro.terminology+json')
    }

    void 'logged in as user -get published models - should return not found'() {

        when:
        loginUser()

        PublishedModelResponse publishedModelResponse = (PublishedModelResponse)  GET(PUBLISHED_MODELS_PATH, PublishedModelResponse)

        then:
        publishedModelResponse
        !publishedModelResponse.publishedModels
    }
}
