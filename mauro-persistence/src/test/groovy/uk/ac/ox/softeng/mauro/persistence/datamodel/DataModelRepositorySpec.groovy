package uk.ac.ox.softeng.mauro.persistence.datamodel

import uk.ac.ox.softeng.mauro.domain.datamodel.DataModel
import uk.ac.ox.softeng.mauro.domain.datamodel.DataType
import uk.ac.ox.softeng.mauro.domain.folder.Folder
import uk.ac.ox.softeng.mauro.domain.terminology.Terminology
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
    AdministeredItemCacheableRepository.DataClassCacheableRepository dataClassRepository

    @Inject
    AdministeredItemCacheableRepository.DataTypeCacheableRepository dataTypeRepository

    @Inject
    AdministeredItemCacheableRepository.EnumerationValueCacheableRepository enumerationValueRepository

    @Inject
    ModelCacheableRepository.FolderCacheableRepository folderRepository

    @Shared
    Folder myFirstFolder

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
                dataClass {
                    label "First class"
                }
                dataClass {
                    label "Second class"
                }
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
            }
            dataModel.setAssociations()
        when:
            DataModel importedModel = dataModelContentRepository.saveWithContent(dataModel)
            importedModel = dataModelRepository.readById(importedModel.id)
        then:
            importedModel.label == "An import model"
            List<DataType> allDataTypes = dataTypeRepository.readAllByParent(importedModel)
            allDataTypes.size() == 1
            enumerationValueRepository.readAllByParent(allDataTypes.get(0)).size() == 2


    }


}
