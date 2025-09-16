package org.maurodata.path

import io.micronaut.http.HttpStatus
import io.micronaut.http.exceptions.HttpStatusException
import io.micronaut.test.annotation.Sql
import jakarta.inject.Singleton
import org.maurodata.domain.model.AdministeredItem
import org.maurodata.domain.terminology.CodeSet
import org.maurodata.persistence.ContainerizedTest
import org.maurodata.testing.CommonDataSpec
import spock.lang.Shared
import spock.lang.Unroll

@ContainerizedTest
@Singleton
@Sql(scripts = "classpath:sql/tear-down.sql", phase = Sql.Phase.AFTER_EACH)
class PathControllerIntegrationSpec extends CommonDataSpec {

    static final String EXPECTED_LABEL = 'test label'
    static final String EXPECTED_PATH = 'fo:Test folder|cs:test label$main'
    @Shared
    UUID folderId

    @Shared
    UUID codeSetId


    def setup() {
        folderId = folderApi.create(folder()).id
        codeSetId = codeSetApi.create(folderId, codeSet(EXPECTED_LABEL)).id
    }

    @Unroll
    void 'test getResource by #path given #domainType -should get resource'() {
        when:
        CodeSet codeSet = pathApi.getResourceByPath(domainType, path) as AdministeredItem as CodeSet
        then:
        codeSet
        codeSet.label
        codeSet.path.pathString == EXPECTED_PATH

        where:
        domainType | path
        'codeSet'  | EXPECTED_PATH
        'codesets' | EXPECTED_PATH
    }


    void 'test getResource by path -path not found -shouldThrowException'() {
        when:
        pathApi.getResourceByPath('datamodel', 'not known label')

        then:
        HttpStatusException exception = thrown()
        exception.status == HttpStatus.NOT_FOUND
    }

    void 'test getResource by path -unknown domainType  -shouldThrowException'() {
        when:
        pathApi.getResourceByPath('whatisthis', EXPECTED_LABEL)

        then:
        HttpStatusException exception = thrown()
        exception.status ==  HttpStatus.NOT_FOUND
    }

}

