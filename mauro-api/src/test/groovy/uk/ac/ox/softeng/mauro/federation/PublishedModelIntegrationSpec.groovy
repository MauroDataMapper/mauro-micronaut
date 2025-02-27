package uk.ac.ox.softeng.mauro.federation

import uk.ac.ox.softeng.mauro.domain.datamodel.DataClass
import uk.ac.ox.softeng.mauro.domain.datamodel.DataModel
import uk.ac.ox.softeng.mauro.domain.facet.federation.PublishedModel
import uk.ac.ox.softeng.mauro.domain.facet.federation.response.PublishedModelResponse
import uk.ac.ox.softeng.mauro.domain.folder.Folder
import uk.ac.ox.softeng.mauro.domain.terminology.CodeSet
import uk.ac.ox.softeng.mauro.domain.terminology.Terminology
import uk.ac.ox.softeng.mauro.persistence.SecuredContainerizedTest
import uk.ac.ox.softeng.mauro.security.SecuredIntegrationSpec

import io.micronaut.runtime.EmbeddedApplication
import io.micronaut.runtime.server.EmbeddedServer
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
        dataModelId = ((DataModel) POST("$FOLDERS_PATH/$folderId$DATAMODELS_PATH", [label: 'data model label'], DataModel)).id
        ((DataClass) POST("$DATAMODELS_PATH/$dataModelId$DATACLASSES_PATH", [label: 'data class label'], DataClass)).id

        terminologyId = ((Terminology) POST("$FOLDERS_PATH/$folderId$TERMINOLOGIES_PATH", [label: 'terminology label'], Terminology)).id
        codeSetId = ((CodeSet) POST("$FOLDERS_PATH/$folderId$CODE_SET_PATH", [label: 'code set label'], CodeSet)).id
        (DataModel) PUT("$DATAMODELS_PATH/$dataModelId/finalise", [version: '1.0.0', versionChangeType: 'major', versionTag: 'versionTagString'], DataModel)
        (Terminology) PUT("$TERMINOLOGIES_PATH/$terminologyId/finalise", [ version: '1.0.0', versionChangeType: 'major', versionTag: 'versionTagString'], Terminology)
        (CodeSet) PUT("$CODE_SET_PATH/$codeSetId/finalise", [version: '1.0.0', versionChangeType: 'major', versionTag: 'versionTagString'], CodeSet)

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
        List<String> publishedModelIds = publishedModelResponse.publishedModels.collect {it.modelId}.toSorted()
        publishedModelIds == sortedFinalisedModelIds
        List<String> linksContentType = publishedModelResponse.publishedModels.collectMany {it.links.collect {it.contentType}}.toSorted()
        linksContentType.size() == 3
        linksContentType == List.of('application/mauro.codeset+json', 'application/mauro.datamodel+json', 'application/mauro.terminology+json')

        List<String> linksUrl = publishedModelResponse.publishedModels.collectMany {it.links.collect {it.url}}.toSorted()
        linksUrl.size() == 3
        PublishedModel terminologyPublishedModel = publishedModelResponse.publishedModels.find{it.modelType == Terminology.class.simpleName }
        terminologyPublishedModel.links?[0].url.contains(terminologyId.toString())
    }
    
    void 'admin user -get published modelsNewerVersions '() {
        given:
        loginAdmin()
        and:
        Terminology newerVersion = (Terminology) PUT("$TERMINOLOGIES_PATH/$terminologyId/newBranchModelVersion", ['version': '1.0.1','branchName': 'main'], Terminology)
        Terminology finalisedNewerVersion = (Terminology) PUT("$TERMINOLOGIES_PATH/$newerVersion.id/finalise", ['version': '2.0.0'], Terminology)

        when:
        PublishedModelResponse publishedModelResponse = (PublishedModelResponse) GET("$PUBLISHED_MODELS_PATH/$terminologyId/newerVersions", PublishedModelResponse)

        then:
        publishedModelResponse
        publishedModelResponse.publishedModels.size() == 1
        PublishedModel publishedModelNewerVersion = publishedModelResponse.publishedModels[0]
        publishedModelNewerVersion.modelId == finalisedNewerVersion.id.toString()
        publishedModelNewerVersion.modelVersion == finalisedNewerVersion.modelVersion
    }

    void 'logged in as user -get published models - should return not found'() {

        when:
        loginUser()

        PublishedModelResponse publishedModelResponse = (PublishedModelResponse) GET(PUBLISHED_MODELS_PATH, PublishedModelResponse)

        then:
        publishedModelResponse
        !publishedModelResponse.publishedModels
    }
}
