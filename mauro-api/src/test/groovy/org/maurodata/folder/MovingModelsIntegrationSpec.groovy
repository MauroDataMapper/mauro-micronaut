package org.maurodata.folder

import io.micronaut.http.HttpStatus
import io.micronaut.http.client.exceptions.HttpClientResponseException
import io.micronaut.test.annotation.Sql
import jakarta.inject.Singleton
import org.maurodata.domain.datamodel.DataModel
import org.maurodata.domain.folder.Folder
import org.maurodata.domain.terminology.CodeSet
import org.maurodata.domain.terminology.Terminology
import org.maurodata.persistence.ContainerizedTest
import org.maurodata.testing.CommonDataSpec
import spock.lang.Shared

@ContainerizedTest
@Singleton
@Sql(scripts = ["classpath:sql/tear-down-datamodel.sql",
    "classpath:sql/tear-down.sql",
    "classpath:sql/tear-down-folder.sql"], phase = Sql.Phase.AFTER_ALL)
class MovingModelsIntegrationSpec extends CommonDataSpec {

    @Shared
    Map<String, UUID> ids = [:]

    void setupSpec() {
        setFolderStructure('Folder 1', false)
        setFolderStructure('Folder 2', true)
        setFolderStructure('Folder 3', true)
    }

    void 'test moving folders between versioned folders - failures'() {

        when:
        folderApi.moveFolder(ids[folderName], ids[destinationName].toString())
        then:

        HttpClientResponseException exception = thrown()
        exception.status == HttpStatus.UNPROCESSABLE_ENTITY

        where:
        folderName              | destinationName
        'Folder 1 / sub-folder' | 'Folder 2 / sub-folder'
        'Folder 1 / sub-folder' | 'Folder 3 / sub-folder'
        'Folder 1 / sub-folder' | 'Folder 2'
        'Folder 1 / sub-folder' | 'Folder 3'
        'Folder 2 / sub-folder' | 'Folder 1 / sub-folder'
        'Folder 2 / sub-folder' | 'Folder 3 / sub-folder'
        'Folder 2 / sub-folder' | 'Folder 1'
        'Folder 2 / sub-folder' | 'Folder 3'
    }

    void 'test moving folders and creating loops - failures'() {

        when:
        folderApi.moveFolder(ids[folderName], ids[destinationName].toString())
        then:

        HttpClientResponseException exception = thrown()
        exception.status == HttpStatus.UNPROCESSABLE_ENTITY

        where:
        folderName              | destinationName
        'Folder 1'              | 'Folder 1 / sub-folder'
        'Folder 2'              | 'Folder 2 / sub-folder'
    }

    void 'test moving data models between versioned folders - failures'() {

        when:
        dataModelApi.moveFolder(ids[dataModelName], ids[destinationName].toString())
        then:

        HttpClientResponseException exception = thrown()
        exception.status == HttpStatus.UNPROCESSABLE_ENTITY

        where:
        dataModelName               | destinationName
        'Folder 1 / data model'     | 'Folder 2'
        'Folder 1 / data model'     | 'Folder 2 / sub-folder'
        'Folder 1 / data model'     | 'Folder 2 / sub-folder / sub-folder'
        'Folder 2 / data model'     | 'Folder 1'
        'Folder 2 / data model'     | 'Folder 3'
        'Folder 2 / data model'     | 'Folder 1 / sub-folder'
        'Folder 2 / data model'     | 'Folder 3 / sub-folder'
        'Folder 2 / data model'     | 'Folder 1 / sub-folder / sub-folder'
        'Folder 2 / data model'     | 'Folder 3 / sub-folder / sub-folder'
    }

    void 'test moving terminologies between versioned folders - failures'() {

        when:
        terminologyApi.moveFolder(ids[terminologyName], ids[destinationName].toString())
        then:

        HttpClientResponseException exception = thrown()
        exception.status == HttpStatus.UNPROCESSABLE_ENTITY

        where:
        terminologyName               | destinationName
        'Folder 1 / terminology'     | 'Folder 2'
        'Folder 1 / terminology'     | 'Folder 2 / sub-folder'
        'Folder 1 / terminology'     | 'Folder 2 / sub-folder / sub-folder'
        'Folder 2 / terminology'     | 'Folder 1'
        'Folder 2 / terminology'     | 'Folder 3'
        'Folder 2 / terminology'     | 'Folder 1 / sub-folder'
        'Folder 2 / terminology'     | 'Folder 3 / sub-folder'
        'Folder 2 / terminology'     | 'Folder 1 / sub-folder / sub-folder'
        'Folder 2 / terminology'     | 'Folder 3 / sub-folder / sub-folder'
    }

    void 'test moving code sets between versioned folders - failures'() {

        when:
        codeSetApi.moveFolder(ids[codeSetName], ids[destinationName].toString())
        then:

        HttpClientResponseException exception = thrown()
        exception.status == HttpStatus.UNPROCESSABLE_ENTITY

        where:
        codeSetName                  | destinationName
        'Folder 1 / code set'        | 'Folder 2'
        'Folder 1 / code set'        | 'Folder 2 / sub-folder'
        'Folder 1 / code set'        | 'Folder 2 / sub-folder / sub-folder'
        'Folder 2 / code set'        | 'Folder 1'
        'Folder 2 / code set'        | 'Folder 3'
        'Folder 2 / code set'        | 'Folder 1 / sub-folder'
        'Folder 2 / code set'        | 'Folder 3 / sub-folder'
        'Folder 2 / code set'        | 'Folder 1 / sub-folder / sub-folder'
        'Folder 2 / code set'        | 'Folder 3 / sub-folder / sub-folder'
    }


    void 'test moving folders between versioned folders - success'() {

        when:
        folderApi.moveFolder(ids[folderName], ids[destinationName].toString())
        then:
        folderApi.list(ids[destinationName]).items.find {it.id == ids[folderName]}

        when: // Move it back
        folderApi.moveFolder(ids[folderName], ids[originalParent].toString())
        then:
        folderApi.list(ids[originalParent]).items.find {it.id == ids[folderName]}


        where:
        folderName                              | originalParent            | destinationName
        'Folder 1 / sub-folder / sub-folder'    | 'Folder 1 / sub-folder'   | 'Folder 1'
        'Folder 2 / sub-folder / sub-folder'    | 'Folder 2 / sub-folder'   | 'Folder 2'
    }

    void 'test moving data models between versioned folders - success'() {

        when:
        dataModelApi.moveFolder(ids[dataModelName], ids[destinationName].toString())
        then:
        dataModelApi.list(ids[destinationName]).items.find {it.id == ids[dataModelName]}

        when: // Move it back
        dataModelApi.moveFolder(ids[dataModelName], ids[originalParent].toString())
        then:
        dataModelApi.list(ids[originalParent]).items.find {it.id == ids[dataModelName]}


        where:
        dataModelName               | originalParent        | destinationName
        'Folder 1 / data model'     | 'Folder 1'            | 'Folder 1 / sub-folder'
        'Folder 2 / data model'     | 'Folder 2'            | 'Folder 2 / sub-folder'
    }

    void 'test moving terminologies between versioned folders - success'() {

        when:
        terminologyApi.moveFolder(ids[terminologyName], ids[destinationName].toString())
        then:
        terminologyApi.list(ids[destinationName]).items.find {it.id == ids[terminologyName]}

        when: // Move it back
        terminologyApi.moveFolder(ids[terminologyName], ids[originalParent].toString())
        then:
        terminologyApi.list(ids[originalParent]).items.find {it.id == ids[terminologyName]}


        where:
        terminologyName               | originalParent        | destinationName
        'Folder 1 / terminology'      | 'Folder 1'            | 'Folder 1 / sub-folder'
        'Folder 2 / terminology'      | 'Folder 2'            | 'Folder 2 / sub-folder'
    }

    void 'test moving code sets between versioned folders - success'() {

        when:
        codeSetApi.moveFolder(ids[codeSetName], ids[destinationName].toString())
        then:
        codeSetApi.list(ids[destinationName]).items.find {it.id == ids[codeSetName]}

        when: // Move it back
        codeSetApi.moveFolder(ids[codeSetName], ids[originalParent].toString())
        then:
        codeSetApi.list(ids[originalParent]).items.find {it.id == ids[codeSetName]}


        where:
        codeSetName                | originalParent        | destinationName
        'Folder 1 / code set'      | 'Folder 1'            | 'Folder 1 / sub-folder'
        'Folder 2 / code set'      | 'Folder 2'            | 'Folder 2 / sub-folder'
    }



    void setFolderStructure(String folderName, Boolean versioned) {

        Folder folder1
        if(versioned){
            folder1 = versionedFolderApi.create(new Folder(label: folderName))
        } else {
            folder1 = folderApi.create(new Folder(label: folderName))
        }
        ids[folderName] = folder1.id

        String subFolderName = folderName + " / sub-folder"
        Folder subFolder = folderApi.create(folder1.id, new Folder(label: subFolderName))
        ids[subFolderName] = subFolder.id

        String subSubFolderName = folderName + " / sub-folder / sub-folder"
        Folder subSubFolder = folderApi.create(subFolder.id, new Folder(label: subSubFolderName))
        ids[subSubFolderName] = subSubFolder.id

        String dataModelName = folderName + " / data model"
        DataModel dataModel = dataModelApi.create(folder1.id, new DataModel(label: dataModelName))
        ids[dataModelName] = dataModel.id

        String terminologyName = folderName + " / terminology"
        Terminology terminology = terminologyApi.create(folder1.id, new Terminology(label: terminologyName))
        ids[terminologyName] = terminology.id

        String codeSetName = folderName + " / code set"
        CodeSet codeSet = codeSetApi.create(folder1.id, new CodeSet(label: codeSetName))
        ids[codeSetName] = codeSet.id

    }

}
