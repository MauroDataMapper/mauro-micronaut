package org.maurodata.folder

import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpStatus
import io.micronaut.test.annotation.Sql
import jakarta.inject.Singleton
import org.maurodata.domain.datamodel.DataClass
import org.maurodata.domain.datamodel.DataModel
import org.maurodata.domain.datamodel.DataType
import org.maurodata.domain.folder.Folder
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

    void 'should delete folder and all associations'() {
        given:
        DataModel dataModel = dataModelApi.create(folderId, dataModelPayload('test data model '))
        DataClass dataClass = dataClassApi.create(dataModel.id, dataClassPayload('test dataclass label'))
        DataType dataType = dataTypeApi.create(dataModel.id, dataTypesPayload())
        dataElementApi.create(dataModel.id, dataClass.id, dataElementPayload('data element label', dataType))

        Terminology terminology = terminologyApi.create(childFolderId, terminology())
        Term term = termApi.create(terminology.id, term())
        CodeSet codeSet = codeSetApi.create(folderId, codeSet())
        codeSetApi.addTerm(codeSet.id, term.id)

        Folder folder = folderApi.show(folderId)

        when:
        HttpResponse response = folderApi.delete(folderId, folder, true )

        then:
        response
        response.status() == HttpStatus.NO_CONTENT

    }


}
