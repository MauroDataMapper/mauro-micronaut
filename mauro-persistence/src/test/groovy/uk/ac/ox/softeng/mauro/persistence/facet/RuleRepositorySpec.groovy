package uk.ac.ox.softeng.mauro.persistence.facet

import uk.ac.ox.softeng.mauro.domain.datamodel.DataModel
import uk.ac.ox.softeng.mauro.domain.facet.Rule
import uk.ac.ox.softeng.mauro.domain.facet.RuleRepresentation
import uk.ac.ox.softeng.mauro.domain.folder.Folder
import uk.ac.ox.softeng.mauro.persistence.ContainerizedTest
import uk.ac.ox.softeng.mauro.persistence.cache.FacetCacheableRepository
import uk.ac.ox.softeng.mauro.persistence.cache.ItemCacheableRepository
import uk.ac.ox.softeng.mauro.persistence.cache.ModelCacheableRepository
import uk.ac.ox.softeng.mauro.persistence.datamodel.DataModelContentRepository
import uk.ac.ox.softeng.mauro.persistence.datamodel.DataModelRepository

import jakarta.inject.Inject
import spock.lang.Shared
import spock.lang.Specification

@ContainerizedTest
class RuleRepositorySpec extends Specification {

    @Inject
    DataModelContentRepository dataModelContentRepository

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
        dataModelId = dataModelContentRepository.saveWithContent(dataModel).id
    }

    def TestRuleAndRepresentations() {
        when:
        DataModel retrievedDataModel = dataModelContentRepository.findWithContentById(dataModelId)

        then:
        retrievedDataModel.rules.size() == 1

        List<RuleRepresentation> ruleRepresentations = ruleRepresentationRepository.findAllByRuleId(retrievedDataModel.rules.first().id)

        retrievedDataModel.rules.first().ruleRepresentations.size() == 2


        retrievedDataModel.rules.first().ruleRepresentations.find { it.language == "English"}
        retrievedDataModel.rules.first().ruleRepresentations.find { it.language == "Java"}
    }

    def TestCacheInvalidation() {
        when:
        DataModel retrievedDataModel = dataModelContentRepository.findWithContentById(dataModelId)

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
