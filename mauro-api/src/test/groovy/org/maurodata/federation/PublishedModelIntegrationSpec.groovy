package org.maurodata.federation

import org.maurodata.domain.datamodel.DataClass
import org.maurodata.domain.datamodel.DataModel
import org.maurodata.domain.facet.federation.PublishedModel
import org.maurodata.domain.facet.federation.response.PublishedModelResponse
import org.maurodata.domain.model.version.CreateNewVersionData
import org.maurodata.domain.model.version.FinaliseData
import org.maurodata.domain.model.version.ModelVersion
import org.maurodata.domain.model.version.VersionChangeType
import org.maurodata.domain.terminology.CodeSet
import org.maurodata.domain.terminology.Terminology
import org.maurodata.persistence.SecuredContainerizedTest
import org.maurodata.security.SecuredIntegrationSpec

import jakarta.inject.Singleton
import spock.lang.Shared

import java.time.Instant

@Singleton
@SecuredContainerizedTest
class PublishedModelIntegrationSpec extends SecuredIntegrationSpec {

    @Shared
    UUID folderId

    @Shared
    UUID dataModelId

    @Shared
    UUID terminologyId
    @Shared
    UUID codeSetId
    @Shared
    Instant startTime

    void setupSpec() {
        loginAdmin()
        startTime = Instant.now()
        folderId = folderApi.create(folder()).id
        dataModelId = dataModelApi.create(folderId, new DataModel(label: 'data model label')).id
        dataClassApi.create(dataModelId, new DataClass(label: 'data class label'))

        terminologyId = terminologyApi.create(folderId, new Terminology(label: 'terminology label')).id
        codeSetId = codeSetApi.create(folderId, new CodeSet(label: 'code set label')).id
        dataModelApi.finalise(dataModelId, new FinaliseData(version: ModelVersion.from('1.0.0'), versionChangeType: VersionChangeType.MAJOR, versionTag: 'versionTagString'))
        terminologyApi.finalise(terminologyId, new FinaliseData(version: ModelVersion.from('1.0.0'), versionChangeType: VersionChangeType.MAJOR, versionTag: 'versionTagString'))
        codeSetApi.finalise(codeSetId, new FinaliseData(version: ModelVersion.from('1.0.0'), versionChangeType: VersionChangeType.MAJOR, versionTag: 'versionTagString'))

        logout()
    }

    void 'admin user -get published models - should return published models'() {
        given:
        loginAdmin()

        List<String> sortedFinalisedModelIds = List.of(codeSetId.toString(), dataModelId.toString(), terminologyId.toString())
        .toSorted()

        when:
        PublishedModelResponse publishedModelResponse = publishApi.show()

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
        PublishedModel terminologyPublishedModel = publishedModelResponse.publishedModels.find {it.modelType == Terminology.class.simpleName}
        terminologyPublishedModel.links ?[0].url.contains(terminologyId.toString())
        terminologyPublishedModel.datePublished >= startTime
        terminologyPublishedModel.lastUpdated >= startTime
    }

    void 'admin user -get published modelsNewerVersions '() {
        given:
        loginAdmin()
        and:
        Terminology newerVersion = terminologyApi.createNewBranchModelVersion(terminologyId, new CreateNewVersionData(label: '1.0.1', branchName: 'main' ))
        Terminology finalisedNewerVersion = terminologyApi.finalise(newerVersion.id, new FinaliseData(version: ModelVersion.from('2.0.0')))

        when:
        PublishedModelResponse publishedModelResponse = publishApi.newerVersions(terminologyId)

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

        PublishedModelResponse publishedModelResponse = publishApi.show()

        then:
        publishedModelResponse
        !publishedModelResponse.publishedModels
    }
}
