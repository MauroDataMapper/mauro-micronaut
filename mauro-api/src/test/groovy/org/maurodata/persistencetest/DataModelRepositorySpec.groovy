package org.maurodata.persistencetest

import io.micronaut.context.annotation.Property
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import jakarta.inject.Inject
import spock.lang.Specification
import org.maurodata.domain.folder.Folder
import org.maurodata.domain.terminology.Terminology
import org.maurodata.persistence.cache.ModelCacheableRepository

@MicronautTest(startApplication = true)
@Property(name = "datasources.default.driver-class-name",
    value = "org.testcontainers.jdbc.ContainerDatabaseDriver")
@Property(name = "datasources.default.url",
    value = "jdbc:tc:postgresql:16-alpine:///db")
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
