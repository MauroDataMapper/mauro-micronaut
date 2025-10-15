package org.maurodata.persistence.facet

import org.maurodata.domain.datamodel.DataModel
import org.maurodata.domain.facet.SummaryMetadataType
import org.maurodata.domain.folder.Folder
import org.maurodata.persistence.ContainerizedTest
import org.maurodata.persistence.ContentsService
import org.maurodata.persistence.cache.FacetCacheableRepository
import org.maurodata.persistence.cache.ModelCacheableRepository
import org.maurodata.persistence.datamodel.DataModelContentRepository
import org.maurodata.persistence.datamodel.DataModelRepository

import jakarta.inject.Inject
import spock.lang.Shared
import spock.lang.Specification

@ContainerizedTest
class SummaryMetadataRepositorySpec extends Specification {

    @Inject
    ContentsService contentsService

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


        dataModelId = contentsService.saveWithContent(dataModel).id
        DataModel retrievedDataModel = contentsService.loadDataModelWithContent(dataModelId)

        then:
        retrievedDataModel.summaryMetadata.size() == 1
        retrievedDataModel.summaryMetadata.first().summaryMetadataReports.size() == 2

        retrievedDataModel.summaryMetadata.first().summaryMetadataReports.find { it.reportValue == "12345"}
        retrievedDataModel.summaryMetadata.first().summaryMetadataReports.find { it.reportValue == "23456"}
    }

}
