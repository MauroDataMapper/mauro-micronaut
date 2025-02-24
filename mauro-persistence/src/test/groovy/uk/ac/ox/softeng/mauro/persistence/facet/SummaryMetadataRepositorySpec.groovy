package uk.ac.ox.softeng.mauro.persistence.facet

import uk.ac.ox.softeng.mauro.domain.datamodel.DataModel
import uk.ac.ox.softeng.mauro.domain.facet.SummaryMetadataType
import uk.ac.ox.softeng.mauro.domain.folder.Folder
import uk.ac.ox.softeng.mauro.persistence.ContainerizedTest
import uk.ac.ox.softeng.mauro.persistence.cache.FacetCacheableRepository
import uk.ac.ox.softeng.mauro.persistence.cache.ModelCacheableRepository
import uk.ac.ox.softeng.mauro.persistence.datamodel.DataModelContentRepository
import uk.ac.ox.softeng.mauro.persistence.datamodel.DataModelRepository

import jakarta.inject.Inject
import spock.lang.Shared
import spock.lang.Specification

@ContainerizedTest
class SummaryMetadataRepositorySpec extends Specification {

    @Inject
    DataModelContentRepository dataModelContentRepository

    @Inject
    ModelCacheableRepository.DataModelCacheableRepository dataModelCacheableRepository

    @Inject
    DataModelRepository dataModelRepository

    @Inject
    ModelCacheableRepository.FolderCacheableRepository folderRepository

    @Inject
    FacetCacheableRepository.SummaryMetadataCacheableRepository summaryMetadataCacheableRepository

    @Shared
    Folder myFirstFolder

    @Shared
    UUID dataModelId

    def TestRuleAndRepresentations() {
        given:

        myFirstFolder = folderRepository.save(new Folder(
            label: "My first Folder"
        ))

        when:
        DataModel dataModel = DataModel.build {
            label "My first data model"
            description "Description here"
            folder myFirstFolder
            summaryMetadata {
                label "My first summary metadata"
                description "My first description"
                summaryMetadataType(SummaryMetadataType.NUMBER)
                summaryMetadataReport {
                    reportDate "2024-01-01T00:00:00.00Z"
                    reportValue "12345"
                }
                summaryMetadataReport {
                    reportDate "2024-01-02T00:00:00.00Z"
                    reportValue "23456"
                }
            }

            dataClass {
                label "My first data class"
            }
        }


        dataModelId = dataModelContentRepository.saveWithContent(dataModel).id
        DataModel retrievedDataModel = dataModelContentRepository.findWithContentById(dataModelId)

        then:
        retrievedDataModel.summaryMetadata.size() == 1
        retrievedDataModel.summaryMetadata.first().summaryMetadataReports.size() == 2

        retrievedDataModel.summaryMetadata.first().summaryMetadataReports.find { it.reportValue == "12345"}
        retrievedDataModel.summaryMetadata.first().summaryMetadataReports.find { it.reportValue == "23456"}
    }

}
