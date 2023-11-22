package uk.ac.ox.softeng.mauro.controller.test

import uk.ac.ox.softeng.mauro.controller.model.ModelController
import uk.ac.ox.softeng.mauro.domain.terminology.Term
import uk.ac.ox.softeng.mauro.persistence.folder.FolderRepository
import uk.ac.ox.softeng.mauro.persistence.model.ModelContentRepository
import uk.ac.ox.softeng.mauro.persistence.model.ModelRepository
import uk.ac.ox.softeng.mauro.persistence.test.TestDTO
import uk.ac.ox.softeng.mauro.persistence.test.TestRepository
import uk.ac.ox.softeng.mauro.test.Test

import com.fasterxml.jackson.databind.ObjectMapper
import groovy.transform.CompileStatic
import io.micronaut.core.annotation.NonNull
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Post
import io.micronaut.transaction.annotation.Transactional
import jakarta.inject.Inject
import reactor.core.publisher.Mono

@Controller
@CompileStatic
class TestController extends ModelController<Test> {

    @Inject
    ObjectMapper objectMapper

    TestRepository testRepository

    TestController(TestRepository testRepository, FolderRepository folderRepository, ModelContentRepository<Test> modelContentRepository) {
        super(Test, testRepository, folderRepository, modelContentRepository)
        this.testRepository = testRepository
    }

    @Get('/test/{id}')
    Mono<Test> show(UUID id) {
        super.show(id)
    }

    @Get('/testAgg/{id}')
    Mono<TestDTO> showAgg(UUID id) {
        testRepository.findTestDTOById(id).map {
            List<Term> termsParsed = []
            it.terms.each {
                termsParsed.add(objectMapper.treeToValue(it, Term))
            }

            it
        }
    }

    @Transactional
    @Post('/folders/{folderId}/test')
    Mono<Test> create(UUID folderId, @Body @NonNull Test test) {
        super.create(folderId, test)
    }
}
