package org.maurodata.persistence.datamodel

import org.maurodata.domain.datamodel.DataClass
import org.maurodata.domain.datamodel.DataElement
import org.maurodata.domain.datamodel.DataModel
import org.maurodata.domain.datamodel.DataType
import org.maurodata.domain.folder.Folder
import org.maurodata.persistence.ContainerizedTest
import org.maurodata.persistence.ContentsService
import org.maurodata.persistence.cache.AdministeredItemCacheableRepository
import org.maurodata.persistence.cache.ModelCacheableRepository

import jakarta.inject.Inject
import spock.lang.Shared
import spock.lang.Specification


@ContainerizedTest
class DataModelRepositorySpec extends Specification {

    @Inject
    @Shared
    ContentsService contentsService

    @Inject
    ModelCacheableRepository.DataModelCacheableRepository dataModelCacheableRepository

    @Inject
    DataModelRepository dataModelRepository

    @Inject
    AdministeredItemCacheableRepository.DataClassCacheableRepository dataClassCacheableRepository

    @Inject
    DataClassRepository dataClassRepository

    @Inject
    AdministeredItemCacheableRepository.DataElementCacheableRepository dataElementRepository

    @Inject
    AdministeredItemCacheableRepository.DataTypeCacheableRepository dataTypeRepository

    @Inject
    AdministeredItemCacheableRepository.EnumerationValueCacheableRepository enumerationValueRepository

    @Inject
    @Shared
    ModelCacheableRepository.FolderCacheableRepository folderRepository

    @Shared
    Folder myFirstFolder

    @Shared
    UUID dataModelId

    void setup() {
        if (!myFirstFolder) {
            myFirstFolder = folderRepository.save(new Folder(
                label: "My first Folder"
            ))
        }
    }

    def TestDataModel() {
        when:
        DataModel dataModel = DataModel.build {
            label "My first data model"
            description "Description here"
            folder myFirstFolder
        }

        dataModelCacheableRepository.save(dataModel)

        then:
        dataModelCacheableRepository.readAll().size() == 1
    }


    def TestDataModel() {
        when:
            DataModel dataModel = DataModel.build {
                label "My first data model"
                description "Description here"
                folder myFirstFolder
            }

            dataModelCacheableRepository.save(dataModel)

        then:
            dataModelCacheableRepository.readAll().size() == 1
    }

    def testImportAndExportFromCacheableRepo() {
        when:
            DataModel importedModel = contentsService.saveWithContent(testDataModel(myFirstFolder))
            importedModel = dataModelCacheableRepository.readById(importedModel.id)
            dataModelId = importedModel.id

        then:
            importedModel.label == "An import model"
            importedModel.description == "The description goes here"

            List<DataClass> allDataClasses = dataClassRepository.findAllByParent(importedModel)
            allDataClasses.size() == 2

            allDataClasses.find {it.label == "Second class"}.extendsDataClasses.size() == 1
            allDataClasses.find {it.label == "Second class"}.extendsDataClasses.first().label == "First class"

            List<DataElement> allDataElements = dataElementRepository.readAllByParent(allDataClasses.find {it.label == "Second class"})

            allDataElements.size() == 1
            allDataElements.first().dataType.label == "Boolean"


            List<DataType> allDataTypes = dataTypeRepository.readAllByParent(importedModel)
            allDataTypes.size() == 1
            enumerationValueRepository.readAllByParent(allDataTypes.get(0)).size() == 2

    }
    def testImportAndFindWithContent() {
        when:
            DataModel importedModel = contentsService.saveWithContent(testDataModel(myFirstFolder))

            importedModel = dataModelRepository.loadWithContent(importedModel.id)
            dataModelId = importedModel.id
            List<DataElement> allDataElements = (List<DataElement>) importedModel.allDataClasses.dataElements.flatten()
            List<DataType> allDataTypes = importedModel.dataTypes

        then:
            importedModel.label == "An import model"
            importedModel.description == "The description goes here"

            importedModel.allDataClasses.size() == 2

            importedModel.allDataClasses.find {it.label == "Second class"}.extendsDataClasses.size() == 1
            importedModel.allDataClasses.find {it.label == "Second class"}.extendsDataClasses.first().label == "First class"


            allDataElements.size() == 1
            allDataElements.first().dataType.label == "Boolean"


            allDataTypes.size() == 1

            allDataTypes.get(0).enumerationValues.size() == 2

    }

    def "Test Retrieving Data Models by Namespace"() {

        when:
        contentsService.saveWithContent(DataModel.build {
            folder myFirstFolder
            label "Data Model 1"
            metadata ("namespace 1", "key 1", "value 1")
            metadata ("namespace 1", "key 2", "value 2")
        })
        contentsService.saveWithContent(DataModel.build {
            folder myFirstFolder
            label "Data Model 2"
            metadata ("namespace 1", "key 1", "value 1")
            metadata ("namespace 2", "key 2", "value 2")
        })
        contentsService.saveWithContent(DataModel.build {
            folder myFirstFolder
            label "Data Model 3"
            metadata ("namespace 2", "key 1", "value 1")
            metadata ("namespace 2", "key 2", "value 2")
        })

        List<DataModel> dataModels = dataModelRepository.getAllModelsByNamespace("namespace 1")
        //List<DataModel> dataModels = dataModelRepository.readAll()
        then:
        dataModels.size() == 2
        dataModels.label.sort() == ["Data Model 1", "Data Model 2"]

        when:
        dataModels = dataModelRepository.getAllModelsByNamespace("namespace 2")

        then:
        dataModels.size() == 2
        dataModels.label.sort() == ["Data Model 2", "Data Model 3"]

        when:
        dataModels = dataModelRepository.getAllModelsByNamespace("namespace 3")

        then:
        dataModels.size() == 0

    }

    DataModel testDataModel(Folder testFolder) {
        DataModel.build {
            label "An import model"
            description "The description goes here"
            folder testFolder
            enumerationType {
                label "Boolean"
                enumerationValue {
                    key "T"
                    value "True"
                }
                enumerationValue {
                    key "F"
                    value "False"
                }
            }
            dataClass {
                label "First class"
            }
            dataClass {
                label "Second class"
                dataElement {
                    label "My first Data Element"
                    dataType "Boolean"
                }
                extendsDataClass "First class"
                metadata("Test ns", "Test key", "Test value")
            }
        }

    }


}
