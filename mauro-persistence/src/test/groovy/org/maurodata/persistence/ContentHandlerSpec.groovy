package org.maurodata.persistence

import io.micronaut.context.ApplicationContext
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import jakarta.inject.Inject
import org.maurodata.domain.datamodel.DataElement
import org.maurodata.domain.datamodel.DataModel
import org.maurodata.domain.datamodel.DataType
import org.maurodata.domain.facet.Metadata
import org.maurodata.domain.terminology.CodeSet
import org.maurodata.domain.terminology.Term
import org.maurodata.domain.terminology.Terminology
import org.maurodata.persistence.cache.AdministeredItemCacheableRepository
import org.maurodata.persistence.datamodel.DataClassRepository
import org.maurodata.persistence.datamodel.DataModelContentRepository
import org.maurodata.persistence.folder.FolderContentRepository
import org.maurodata.persistence.folder.dto.FolderDTORepository
import org.maurodata.persistence.terminology.CodeSetContentRepository
import spock.lang.Specification

import org.maurodata.domain.datamodel.DataClass
import org.maurodata.domain.folder.Folder
import org.maurodata.persistence.cache.ModelCacheableRepository

import java.time.Duration
import java.time.Instant

@ContainerizedTest
//@MicronautTest
class ContentHandlerSpec extends Specification{

    @Inject
    ModelCacheableRepository.FolderCacheableRepository folderCacheableRepository

    @Inject
    FolderDTORepository folderDTORepository

    @Inject
    FolderContentRepository folderContentRepository

    @Inject
    ModelCacheableRepository.DataModelCacheableRepository dataModelCacheableRepository

    @Inject
    ModelCacheableRepository.TerminologyCacheableRepository terminologyCacheableRepository

    @Inject
    ModelCacheableRepository.CodeSetCacheableRepository codeSetCacheableRepository

    @Inject
    CodeSetContentRepository codeSetContentRepository

    @Inject
    AdministeredItemCacheableRepository.TermCacheableRepository termCacheableRepository

    @Inject
    AdministeredItemCacheableRepository.DataClassCacheableRepository dataClassCacheableRepository

    @Inject DataClassRepository dataClassRepository

    @Inject
    AdministeredItemCacheableRepository.DataElementCacheableRepository dataElementCacheableRepository

    @Inject
    AdministeredItemCacheableRepository.DataTypeCacheableRepository dataTypeCacheableRepository

    @Inject
    AdministeredItemCacheableRepository.EnumerationValueCacheableRepository enumerationValueCacheableRepository

    @Inject
    ContentsService contentsService

    void "test saving a folder and its content"() {

        when:



        Folder folder = getBigFolder()
        contentsService.saveWithContent(folder)

        Instant start = Instant.now()


        then:
        folder.id

        when:
        List<Folder> folders1 = folderCacheableRepository.readAll()
        then:
        folders1.size() == 4

        when:
        List<Terminology> terminologies1 = terminologyCacheableRepository.readAll()
        then:
        terminologies1.size() == 2

        when:
        List<Term> terms1 = termCacheableRepository.readAllByParent(terminologies1.first())
        then:
        terms1.size() == 10

        when:
        List<CodeSet> codeSets = codeSetCacheableRepository.readAll()
        then:
        codeSets.size() == 1

        when:
        CodeSet codeSet = codeSetContentRepository.readWithContentById(codeSets.first().id)
        then:
        codeSet.terms.size() == 5

        when:
        List<DataModel> dataModels = dataModelCacheableRepository.readAll()
        then:
        dataModels.size() == 1

        when:
        Map<String, DataType> dataTypes = dataTypeCacheableRepository.readAllByParent(dataModels.first()).collectEntries {
            [it.label, it]
        }

        List<DataClass> dataClasses = dataClassCacheableRepository.readAllByDataModelAndParentDataClassIsNull(dataModels.first())
        then:
        // Top level data classes
        dataClasses.size() == 4

        UUID employerId = dataClasses.find {it.label == "Employer"}.id
        dataClassRepository.getDataClassExtensionRelationships(employerId).size() == 1
        DataClass personDataClass = dataClasses.find {it.label == "Person"}
        DataClass companyDataClass = dataClasses.find {it.label == "Company"}
        DataClass employerDataClass = dataClasses.find {it.label == "Employer"}

        when:
        dataClasses = dataClassCacheableRepository.readAllByParentDataClass(dataClasses.find {it.label == "Person"})
        then:
        dataClasses.size() == 1

        when:
        dataClasses = dataClassCacheableRepository.readAllByParentDataClass(personDataClass)
        then:
        dataClasses.size() == 1

        List<DataElement> dataElements = dataElementCacheableRepository.readAllByDataClass_Id(personDataClass.id)
        dataElements.size() == 3

        dataElements.find {it.label == "name"}.dataType.id == dataTypes['String'].id
        dataElements.find {it.label == "gender"}.dataType.id == dataTypes['Gender'].id
        dataElements.find {it.label == "worksFor"}.dataType.id == dataTypes['Reference to Company'].id
        dataTypes['Reference to Company'].referenceClass.id == companyDataClass.id

        when:
        dataElements = dataElementCacheableRepository.readAllByDataClass_Id(employerDataClass.id)

        then:
        dataElements.find {it.label == "Favourite Terminology"}.dataType.id == dataTypes['Reference to Terminology 0'].id
        dataTypes['Reference to Terminology 0'].modelResourceId == terminologies1.first().id
        dataElements.find {it.label == "Favourite CodeSet"}.dataType.id == dataTypes['Reference to My first codeSet'].id
        dataTypes['Reference to My first codeSet'].modelResourceId == codeSet.id

        enumerationValueCacheableRepository.readAllByEnumerationType_Id(dataTypes['Gender'].id).size() == 2



    }

    void "test saving a folder and its content - old"() {

        when:

        Folder folder = getBigFolder()

        Instant start = Instant.now()
        contentsService.saveWithContent(folder)
        Duration timeTaken = Duration.between(start, Instant.now())
        printTimeTaken(start)

        then:
        folder.id

        when:
        List<Folder> folders1 = folderCacheableRepository.readAll()
        then:
        folders1.size() == 4

        when:
        List<Terminology> terminologies1 = terminologyCacheableRepository.readAll()
        then:
        terminologies1.size() == 2

        when:
        List<Term> terms1 = termCacheableRepository.readAllByParent(terminologies1.first())
        then:
        terms1.size() == 10

        when:
        List<CodeSet> codeSets = codeSetCacheableRepository.readAll()
        then:
        codeSets.size() == 1

        when:
        CodeSet codeSet = codeSetContentRepository.readWithContentById(codeSets.first().id)
        then:
        codeSet.terms.size() == 5

        when:
        List<DataModel> dataModels = dataModelCacheableRepository.readAll()
        then:
        dataModels.size() == 1

        when:
        List<DataClass> dataClasses = dataClassCacheableRepository.readAllByDataModelAndParentDataClassIsNull(dataModels.first())
        then:
        // Top level data classes
        dataClasses.size() == 4



    }


    Folder getBigFolder() {
        List<Term> linkedTerms = []
        Folder folder = Folder.build {
            label 'Top level folder'
            folder {
                label 'Child folder 1'
            }
            folder {
                label 'Child folder 2'
                folder {
                    label 'Child folder 2.1'
                    (0..1).each { tyIdx ->
                        terminology (label: "Terminology $tyIdx") {
                            (0..9).each {tmIdx ->
                                term (code: "T$tmIdx", definition: "Term $tmIdx") {
                                    (0..9).each {mdIdx ->
                                        metadata("test namespace",
                                                 "key $mdIdx",
                                                 "value $mdIdx")
                                    }
                                }
                            }
                            linkedTerms.addAll(terms)
                        }
                    }

                }
            }
            codeSet(label: 'My first codeSet') {
                terms.addAll(linkedTerms.take(5))
            }
            dataModel {
                label "My first dataModel"
                primitiveType (label: "String")
                enumerationType (label: "Gender") {
                    enumerationValue(key: "M", value: "Male")
                    enumerationValue(key: "F", value: "Female")
                }
                dataClass (label: "Company") {
                    dataElement {
                        label "name"
                        dataType "String"
                    }
                }

                dataClass (label: "Person") {
                    dataElement {
                        label "name"
                        dataType "String"
                    }
                    dataElement {
                        label "gender"
                        dataType "Gender"
                    }
                    dataClass {
                        label "address"
                        dataElement {
                            label "address"
                            dataType "String"
                        }
                    }
                    dataElement {
                        label "worksFor"
                        referenceType "Company"
                    }
                }
                dataClass (label: "Employee") {
                    extendsDataClass "Person"
                }
                dataClass (label: "Employer") {
                    extendsDataClass "Company"
                    dataElement {
                        label "Favourite Terminology"
                    }
                    dataElement {
                        label "Favourite CodeSet"
                    }
                }
            }
        }
        Terminology referenceTerminology = folder.childFolders
            .find {it.label == 'Child folder 2'}
            .childFolders.find {it.label == 'Child folder 2.1'}
            .terminologies.find {it.label == 'Terminology 0'}
        DataType dataType = new DataType(label: "Reference to Terminology 0",
                     dataModel: folder.dataModels.first(),
                     dataTypeKind: DataType.DataTypeKind.MODEL_TYPE,
                     modelResource: referenceTerminology)

        folder.dataModels.first().dataElements.find {it.label == "Favourite Terminology"}.dataType = dataType
        folder.dataModels.first().dataTypes.add(dataType)
        CodeSet referenceCodeSet = folder.codeSets.find {it.label == 'My first codeSet'}
        DataType codeSetDataType = new DataType(label: "Reference to My first codeSet",
                                         dataModel: folder.dataModels.first(),
                                         dataTypeKind: DataType.DataTypeKind.MODEL_TYPE,
                                         modelResource: referenceCodeSet)

        folder.dataModels.first().dataElements.find {it.label == "Favourite CodeSet"}.dataType = codeSetDataType
        folder.dataModels.first().dataTypes.add(codeSetDataType)
        return folder
    }

    void printTimeTaken(Instant start) {
        Duration timeTaken = Duration.between(start, Instant.now())
        System.err.println(String.format("Time taken: %sm %ss %sms",
                                         timeTaken.toMinutesPart(),
                                         timeTaken.toSecondsPart(),
                                         timeTaken.toMillisPart()))

    }


}
