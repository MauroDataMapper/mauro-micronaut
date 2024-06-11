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
    DataModelContentRepository dataModelContentRepository

    @Inject
    ModelCacheableRepository.DataModelCacheableRepository dataModelRepository

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
    ModelCacheableRepository.FolderCacheableRepository folderRepository

    @Shared
    Folder myFirstFolder

    @Shared
    UUID dataModelId

    def TestDataModel() {
        given:

        myFirstFolder = folderRepository.save(new Folder(
            label: "My first Folder"
        ))

        when:
        DataModel dataModel = DataModel.build {
            label "My first data model"
            description "Description here"
            folder myFirstFolder

        }

        dataModelRepository.save(dataModel)

        then:
        dataModelRepository.readAll().size() == 1
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
                }
            }

        when:
            DataModel importedModel = dataModelContentRepository.saveWithContent(dataModel)
            importedModel = dataModelRepository.readById(importedModel.id)
            dataModelId = importedModel.id
        then:
            importedModel.label == "An import model"
            importedModel.description == "The description goes here"

            List<DataClass> allDataClasses = dataClassCacheableRepository.readAllByParent(importedModel)
            allDataClasses.size() == 2


            List<DataElement> allDataElements = dataElementRepository.readAllByParent(allDataClasses.find {it.label == "Second class"})

            allDataElements.size() == 1
            allDataElements.first().dataType.label == "Boolean"


            List<DataType> allDataTypes = dataTypeRepository.readAllByParent(importedModel)
            allDataTypes.size() == 1
            enumerationValueRepository.readAllByParent(allDataTypes.get(0)).size() == 2



    }
    
}
