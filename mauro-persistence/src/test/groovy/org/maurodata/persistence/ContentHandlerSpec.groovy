package org.maurodata.persistence

import io.micronaut.context.ApplicationContext
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import jakarta.inject.Inject
import org.maurodata.domain.facet.Metadata
import org.maurodata.domain.terminology.Term
import org.maurodata.domain.terminology.Terminology
import org.maurodata.persistence.cache.AdministeredItemCacheableRepository
import org.maurodata.persistence.folder.FolderContentRepository
import org.maurodata.persistence.folder.dto.FolderDTORepository
import spock.lang.Specification

import org.maurodata.domain.datamodel.DataClass
import org.maurodata.domain.folder.Folder
import org.maurodata.persistence.cache.ModelCacheableRepository

import java.time.Duration
import java.time.Instant

//@ContainerizedTest
@MicronautTest
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
    AdministeredItemCacheableRepository.TermCacheableRepository termCacheableRepository

    @Inject
    ApplicationContext applicationContext

    void "test saving a folder and its content"() {

        when:

        Folder folder = getBigFolder()

        Instant start = Instant.now()
        ContentHandler contentHandler = applicationContext.createBean(ContentHandler)
        contentHandler.shred(folder)
        //printTimeTaken(start)
        contentHandler.saveWithContent()
        //printTimeTaken(start)

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
        terms1.size() == 2


    }

    void "test saving a folder and its content - old"() {

        when:

        Folder folder = getBigFolder()

        Instant start = Instant.now()
        folderContentRepository.saveWithContent(folder)
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
        terminologies1.size() == 1000

        when:
        List<Term> terms1 = termCacheableRepository.readAllByParent(terminologies1.first())
        then:
        terms1.size() == 10


    }


    Folder getBigFolder() {
        Folder.build {
            label 'Top level folder'
            folder {
                label 'Child folder 1'
            }
            folder {
                label 'Child folder 2'
                folder {
                    label 'Child folder 2.1'
                    (0..999).each { tyIdx ->
                        terminology (label: "Terminology $tyIdx") {
                            (0..9).each {tmIdx ->
                                term (code: "T$tmIdx", definition: "Term $tmIdx") {
                                    (0..9).each {mdIdx ->
                                        metadata(new Metadata(namespace: "test namespace",
                                                  key: "key $mdIdx",
                                                  value: "value $mdIdx"))
                                    }
                                }
                            }
                        }
                    }

                }
            }
        }
    }

    void printTimeTaken(Instant start) {
        Duration timeTaken = Duration.between(start, Instant.now())
        System.err.println(String.format("Time taken: %sm %ss %sms",
                                         timeTaken.toMinutesPart(),
                                         timeTaken.toSecondsPart(),
                                         timeTaken.toMillisPart()))

    }


}
