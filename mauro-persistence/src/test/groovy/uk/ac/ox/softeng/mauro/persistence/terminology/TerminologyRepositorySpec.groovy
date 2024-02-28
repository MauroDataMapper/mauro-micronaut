package uk.ac.ox.softeng.mauro.persistence.terminology

import uk.ac.ox.softeng.mauro.domain.folder.Folder
import uk.ac.ox.softeng.mauro.domain.terminology.Terminology
import uk.ac.ox.softeng.mauro.persistence.ContainerizedTest
import uk.ac.ox.softeng.mauro.persistence.cache.ModelCacheableRepository

import jakarta.inject.Inject
import spock.lang.Specification


@ContainerizedTest
class TerminologyRepositorySpec extends Specification {

    @Inject
    ModelCacheableRepository.TerminologyCacheableRepository terminologyRepository

    @Inject
    ModelCacheableRepository.FolderCacheableRepository folderRepository

    def TestTerminology() {
        given:

        Folder folder = folderRepository.save(new Folder(
            label: "My first Folder"
        ))

        when:
        Terminology terminology = Terminology.build {
            label "My first terminology"
            description "Description here"
        }
        terminology.folder = folder

        terminologyRepository.save(terminology)

        then:
        terminologyRepository.readAll().size() == 1
    }
}
