package uk.ac.ox.softeng.mauro.persistence.datamodel

import uk.ac.ox.softeng.mauro.domain.datamodel.DataClass
import uk.ac.ox.softeng.mauro.domain.datamodel.DataElement
import uk.ac.ox.softeng.mauro.domain.datamodel.DataModel
import uk.ac.ox.softeng.mauro.domain.datamodel.DataType
import uk.ac.ox.softeng.mauro.domain.folder.Folder
import uk.ac.ox.softeng.mauro.persistence.ContainerizedTest
import uk.ac.ox.softeng.mauro.persistence.cache.AdministeredItemCacheableRepository
import uk.ac.ox.softeng.mauro.persistence.cache.ModelCacheableRepository

import jakarta.inject.Inject
import spock.lang.Shared
import spock.lang.Specification


@ContainerizedTest
class DataModelRepositorySpec extends Specification {

    @Inject
    @Shared
    DataModelContentRepository dataModelContentRepository

    @Inject
    @Shared
    DataClassContentRepository dataClassContentRepository

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

    def testImport() {
        given:
            DataModel dataModel = DataModel.build {
                label "An import model"
                description "The description goes here"
                folder myFirstFolder
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

        when:
            DataModel importedModel = dataModelContentRepository.saveWithContent(dataModel)
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

    def "Test Retrieving Data Models by Namespace"() {

        when:
        dataModelContentRepository.saveWithContent(DataModel.build {
            folder myFirstFolder
            label "Data Model 1"
            metadata ("namespace 1", "key 1", "value 1")
            metadata ("namespace 1", "key 2", "value 2")
        })
        dataModelContentRepository.saveWithContent(DataModel.build {
            folder myFirstFolder
            label "Data Model 2"
            metadata ("namespace 1", "key 1", "value 1")
            metadata ("namespace 2", "key 2", "value 2")
        })
        dataModelContentRepository.saveWithContent(DataModel.build {
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
    
}
