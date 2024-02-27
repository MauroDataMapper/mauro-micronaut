package uk.ac.ox.softeng.mauro.persistencetest

import uk.ac.ox.softeng.mauro.domain.folder.Folder
import uk.ac.ox.softeng.mauro.domain.terminology.Terminology
import uk.ac.ox.softeng.mauro.persistence.cache.ModelCacheableRepository

import io.micronaut.context.annotation.Property
import io.micronaut.core.io.ResourceLoader
import io.micronaut.test.annotation.Sql
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import jakarta.inject.Inject
import spock.lang.Specification

@MicronautTest(startApplication = true)
@Property(name = "datasources.default.driver-class-name",
    value = "org.testcontainers.jdbc.ContainerDatabaseDriver")
@Property(name = "datasources.default.url",
    value = "jdbc:tc:postgresql:15.2-alpine:///db")
class DataModelRepositorySpec extends Specification {

//    @Inject
//    DataModelRepository dataModelRepository

//    @Inject
//    Connection connection

    @Inject
    ModelCacheableRepository.TerminologyCacheableRepository terminologyRepository

    @Inject
    ModelCacheableRepository.FolderCacheableRepository folderRepository


    /*    def "Test try storing a datamodel"() {

            when:
                DataModel dataModel = DataModel.build {
                    label "My first datamodel"
                    description "Description here"
                }

                dataModelRepository.save(dataModel)
            then:
                true
        }
    */

//    def "Test try storing a terminology"() {

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
