package org.maurodata.facet

import org.maurodata.domain.datamodel.DataClass
import org.maurodata.domain.datamodel.DataModel
import org.maurodata.domain.model.version.CreateNewVersionData
import org.maurodata.domain.model.version.FinaliseData
import org.maurodata.domain.model.version.ModelVersion

import io.micronaut.core.annotation.NonNull
import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpStatus
import io.micronaut.http.annotation.Body
import jakarta.inject.Singleton
import org.maurodata.api.facet.SemanticLinkCreateDTO
import org.maurodata.api.facet.SemanticLinkDTO
import org.maurodata.domain.facet.SemanticLink
import org.maurodata.domain.folder.Folder
import org.maurodata.persistence.ContainerizedTest
import org.maurodata.testing.CommonDataSpec
import org.maurodata.web.ListResponse
import spock.lang.Shared

@ContainerizedTest
@Singleton
class SemanticLinkIntegrationSpec extends CommonDataSpec {

    @Shared
    UUID folderId

    @Shared
    UUID dataModelId

    @Shared
    UUID dataClassId_1

    @Shared
    UUID dataClassId_2

    void setup() {
        folderId = folderApi.create(folder()).id
        dataModelId = dataModelApi.create(folderId, dataModelPayload()).id
        dataClassId_1 = dataClassApi.create(dataModelId, dataClassPayload("Data class 1")).id
        dataClassId_2 = dataClassApi.create(dataModelId, dataClassPayload("Data class 2")).id
    }

    void 'list empty semantic links'() {
        when:
        ListResponse<SemanticLinkDTO> semanticLinks = semanticLinksApi.list("dataClasses", dataClassId_1)

        then:
        semanticLinks.count == 0
    }

    void 'create semantic link'() {
        when:
        SemanticLinkCreateDTO semanticLinkToCreate = new SemanticLinkCreateDTO(linkType: "Refines", targetMultiFacetAwareItemDomainType: "DataClass", targetMultiFacetAwareItemId: dataClassId_2)
        SemanticLinkDTO createdSemanticLink = semanticLinksApi.create("DataClass", dataClassId_1, semanticLinkToCreate)
        then:
        createdSemanticLink
        createdSemanticLink.id
        createdSemanticLink.linkType == "Refines"
        createdSemanticLink.domainType == "SemanticLink"
        createdSemanticLink.unconfirmed == false
        createdSemanticLink.sourceMultiFacetAwareItem
        createdSemanticLink.sourceMultiFacetAwareItem.id == dataClassId_1
        createdSemanticLink.sourceMultiFacetAwareItem.label == "Data class 1"
        createdSemanticLink.sourceMultiFacetAwareItem.domainType == "DataClass"
        createdSemanticLink.targetMultiFacetAwareItem
        createdSemanticLink.targetMultiFacetAwareItem.id == dataClassId_2
        createdSemanticLink.targetMultiFacetAwareItem.label == "Data class 2"
        createdSemanticLink.targetMultiFacetAwareItem.domainType == "DataClass"

        when:
        ListResponse<SemanticLinkDTO> semanticLinks = semanticLinksApi.list("dataClasses", dataClassId_1)

        then:
        semanticLinks.count == 1
    }


    void 'clone model with semantic link'() {
        when:

        SemanticLinkCreateDTO semanticLinkToCreate = new SemanticLinkCreateDTO(linkType: "Refines", targetMultiFacetAwareItemDomainType: "DataClass", targetMultiFacetAwareItemId: dataClassId_2)
        SemanticLinkDTO createdSemanticLink = semanticLinksApi.create("DataClass", dataClassId_1, semanticLinkToCreate)

        dataModelApi.finalise(dataModelId, new FinaliseData(version: ModelVersion.from("1.0.0")))
        DataModel clonedDataModel = dataModelApi.createNewBranchModelVersion(dataModelId, new CreateNewVersionData(branchName: "main"))

        then:
        clonedDataModel.id != dataModelId

        when:
        List<DataClass> clonedDataClasses = dataClassApi.allDataClasses(clonedDataModel.id).items
        DataClass clonedDataClass1 = clonedDataClasses.find {it.label == "Data class 1"}
        DataClass clonedDataClass2 = clonedDataClasses.find {it.label == "Data class 2"}

        then:
        clonedDataClass1.id != dataClassId_1
        clonedDataClass2.id != dataClassId_2

        when:
        List<SemanticLinkDTO> clonedSemanticLinks = semanticLinksApi.list("dataClass", clonedDataClass1.id).items

        then:
        clonedSemanticLinks.size() == 1
        clonedSemanticLinks[0].sourceMultiFacetAwareItem.id == clonedDataClass1.id
        clonedSemanticLinks[0].targetMultiFacetAwareItem.id == clonedDataClass2.id


    }
}
