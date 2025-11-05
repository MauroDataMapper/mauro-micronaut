package org.maurodata.persistence.facet

import org.maurodata.domain.datamodel.DataModel
import org.maurodata.domain.facet.RuleRepresentation
import org.maurodata.domain.folder.Folder
import org.maurodata.persistence.ContainerizedTest
import org.maurodata.persistence.ContentsService
import org.maurodata.persistence.cache.FacetCacheableRepository
import org.maurodata.persistence.cache.ItemCacheableRepository
import org.maurodata.persistence.cache.ModelCacheableRepository

import org.maurodata.persistence.datamodel.DataModelRepository

import jakarta.inject.Inject
import spock.lang.Shared
import spock.lang.Specification

@ContainerizedTest
class RuleRepositorySpec extends Specification {

    @Inject
    ContentsService contentsService

    @Inject
    ModelCacheableRepository.DataModelCacheableRepository dataModelCacheableRepository

    @Inject
    DataModelRepository dataModelRepository

    @Inject
    ModelCacheableRepository.FolderCacheableRepository folderRepository

    @Inject
    FacetCacheableRepository.RuleCacheableRepository ruleCacheableRepository

    @Inject
    RuleRepresentationRepository ruleRepresentationRepository

    @Inject
    ItemCacheableRepository.RuleRepresentationCacheableRepository ruleRepresentationCacheableRepository

    @Shared
    Folder myFirstFolder

    @Shared
    UUID dataModelId

    void setup() {
        myFirstFolder = folderRepository.save(new Folder(
                label: "My first Folder"
        ))

        DataModel dataModel = DataModel.build {
            label "My first data model"
            description "Description here"
            folder myFirstFolder
            rule {
                name "Age is sensible"
                description "All ages should be greater than or equal to zero"
                ruleRepresentation {
                    language "English"
                    representation "Age is not less than zero"
                }
                ruleRepresentation {
                    language "Java"
                    representation "age >= 0"
                }
            }
            dataClass {
                label "My first data class"
            }
        }
        dataModelId = contentsService.saveWithContent(dataModel).id
    }

    def TestRuleAndRepresentations() {
        when:
        DataModel retrievedDataModel = dataModelRepository.loadWithContent(dataModelId)

        then:
        retrievedDataModel.rules.size() == 1

        List<RuleRepresentation> ruleRepresentations = ruleRepresentationRepository.findAllByRuleId(retrievedDataModel.rules.first().id)

        retrievedDataModel.rules.first().ruleRepresentations.size() == 2


        retrievedDataModel.rules.first().ruleRepresentations.find { it.language == "English"}
        retrievedDataModel.rules.first().ruleRepresentations.find { it.language == "Java"}
    }

    def TestCacheInvalidation() {
        when:
        DataModel retrievedDataModel = dataModelRepository.loadWithContent(dataModelId)

        then:
        retrievedDataModel.rules.size() == 1

        List<RuleRepresentation> ruleRepresentations = ruleRepresentationRepository.findAllByRuleId(retrievedDataModel.rules.first().id)

        retrievedDataModel.rules.first().ruleRepresentations.size() == 2
        UUID ruleId = retrievedDataModel.rules.first().id

        when:
        ruleRepresentationRepository.delete(retrievedDataModel.rules.first().ruleRepresentations.first())
        retrievedDataModel = dataModelCacheableRepository.findById(retrievedDataModel.id)

        then:
        retrievedDataModel.rules.size() == 1
        retrievedDataModel.rules.first().ruleRepresentations.size() == 1
    }

}
