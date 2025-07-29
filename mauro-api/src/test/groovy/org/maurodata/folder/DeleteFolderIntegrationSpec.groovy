package org.maurodata.folder

import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpStatus
import io.micronaut.test.annotation.Sql
import jakarta.inject.Singleton
import org.maurodata.api.facet.SemanticLinkCreateDTO
import org.maurodata.domain.datamodel.DataClass
import org.maurodata.domain.datamodel.DataElement
import org.maurodata.domain.datamodel.DataModel
import org.maurodata.domain.datamodel.DataType
import org.maurodata.domain.folder.Folder
import org.maurodata.domain.model.version.FinaliseData
import org.maurodata.domain.model.version.VersionChangeType
import org.maurodata.domain.terminology.CodeSet
import org.maurodata.domain.terminology.Term
import org.maurodata.domain.terminology.Terminology
import org.maurodata.persistence.ContainerizedTest
import org.maurodata.testing.CommonDataSpec
import spock.lang.Shared

@ContainerizedTest
@Singleton
@Sql(scripts = ["classpath:sql/tear-down-datamodel.sql",
    "classpath:sql/tear-down.sql",
    "classpath:sql/tear-down-folder.sql"], phase = Sql.Phase.AFTER_ALL)
class DeleteFolderIntegrationSpec extends CommonDataSpec {

    @Shared
    UUID folderId
    @Shared
    UUID childFolderId
   

    void setup() {
        folderId = folderApi.create(folder()).id
        childFolderId = folderApi.create(folderId, folder('child label')).id

    }

    void 'should delete folder and all associations - happy path'() {
        given:
        DataModel dataModel = dataModelApi.create(folderId, dataModelPayload('test data model '))
        DataClass dataClass = dataClassApi.create(dataModel.id, dataClassPayload('test dataclass label'))
        DataType dataType = dataTypeApi.create(dataModel.id, dataTypesPayload())
        DataElement dataElement = dataElementApi.create(dataModel.id, dataClass.id, dataElementPayload('data element label', dataType))
        annotationApi.create("dataElement", dataElement.id, annotationPayload('label for annotation ', 'description'))
        Terminology terminology = terminologyApi.create(childFolderId, terminologyPayload())
        Term term = termApi.create(terminology.id, term())
        summaryMetadataApi.create("term", term.id, summaryMetadataPayload())
        CodeSet codeSet = codeSetApi.create(folderId, codeSet())
        codeSetApi.addTerm(codeSet.id, term.id)

        Folder folder = folderApi.show(folderId)

        when:
        HttpResponse response = folderApi.delete(folderId, folder, true )

        then:
        response
        response.status() == HttpStatus.NO_CONTENT
    }

    void 'should delete folder and all associations - folder has reference to finalised model belonging to different folder'() {
        given:
        DataModel dataModel = dataModelApi.create(folderId, dataModelPayload('test data model '))
        Folder otherFolder = folderApi.create(folder('other folder'))
        Terminology terminology = terminologyApi.create(otherFolder.id, terminologyPayload())
        terminologyApi.finalise(terminology.id, new FinaliseData().tap{
            versionChangeType = VersionChangeType.MAJOR
            versionTag = '2.2'
        } )

        dataTypeApi.create(dataModel.id, modelTypeDataTypePayload(terminology.id, Terminology.class.simpleName))

        Folder folder = folderApi.show(folderId)

        when:
        HttpResponse response = folderApi.delete(folderId, folder, true )

        then:
        response
        response.status() == HttpStatus.NO_CONTENT
    }

    void 'delete folder  - folder has reference to not finalised model belonging to different folder'() {
        given:
        dataModelApi.create(folderId, dataModelPayload('test data model '))
        Folder otherFolder = folderApi.create(folder('other folder'))
        Terminology terminology = terminologyApi.create(otherFolder.id, terminologyPayload())

        Terminology terminology2 = terminologyApi.create(otherFolder.id, terminologyPayload('terminnology 2 label'))

        summaryMetadataApi.create("terminology", terminology.id, summaryMetadataPayload())

        semanticLinksApi.create(Terminology.class.simpleName, terminology2.id, new SemanticLinkCreateDTO().tap{
            linkType = 'Refines'
            targetMultiFacetAwareItemDomainType = Terminology.class.simpleName
            targetMultiFacetAwareItemId  = terminology.id
        })

        Folder folder = folderApi.show(folderId)

        when:
        HttpResponse response = folderApi.delete(folderId, folder, true )

        then:
        response
        response.status() == HttpStatus.NO_CONTENT
    }

}
