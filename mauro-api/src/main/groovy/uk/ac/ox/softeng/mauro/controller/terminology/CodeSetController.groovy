package uk.ac.ox.softeng.mauro.controller.terminology

import com.fasterxml.jackson.databind.ObjectMapper
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import io.micronaut.core.annotation.NonNull
import io.micronaut.core.annotation.Nullable
import io.micronaut.http.HttpStatus
import io.micronaut.http.annotation.*
import io.micronaut.transaction.annotation.Transactional
import jakarta.inject.Inject
import uk.ac.ox.softeng.mauro.controller.model.ModelController
import uk.ac.ox.softeng.mauro.domain.model.version.CreateNewVersionData
import uk.ac.ox.softeng.mauro.domain.model.version.FinaliseData
import uk.ac.ox.softeng.mauro.domain.terminology.CodeSet
import uk.ac.ox.softeng.mauro.domain.terminology.CodeSetService
import uk.ac.ox.softeng.mauro.persistence.cache.ModelCacheableRepository
import uk.ac.ox.softeng.mauro.persistence.terminology.CodeSetContentRepository
import uk.ac.ox.softeng.mauro.web.ListResponse

@Slf4j
@Controller
@CompileStatic
class CodeSetController extends ModelController<CodeSet> {

    ModelCacheableRepository.CodeSetCacheableRepository codeSetRepository

    CodeSetContentRepository codeSetContentRepository

    @Inject
    CodeSetService codeSetService

    @Inject
    ObjectMapper objectMapper

    CodeSetController(ModelCacheableRepository.CodeSetCacheableRepository codeSetRepository, ModelCacheableRepository.FolderCacheableRepository folderRepository, CodeSetContentRepository codeSetContentRepository) {
        super(CodeSet, codeSetRepository, folderRepository, codeSetContentRepository)
        this.codeSetRepository = codeSetRepository
        this.codeSetContentRepository = codeSetContentRepository
    }


    @Get(value = Paths.CODE_SET_BY_ID)
    CodeSet show(UUID id) {
        log.debug("********   CodeSetController:  show  ${id}")
        super.show(id)
    }

    @Transactional
    @Post(value = Paths.CODE_SETS_BY_FOLDER_ID)
    CodeSet create(UUID folderId, @Body @NonNull CodeSet codeSet) {
        super.create(folderId, codeSet)
    }

    @Put(value = Paths.CODE_SET_BY_ID)
    CodeSet update(UUID id, @Body @NonNull CodeSet codeSet) {
        super.update(id, codeSet)
    }

    @Transactional
    @Delete(value = Paths.CODE_SET_BY_ID)
    HttpStatus delete(UUID id, @Body @Nullable CodeSet codeSet) {
        log.debug("**********  CodeSetController: deleting ${id}")
        super.delete(id, codeSet)
    }

    @Get(value = Paths.CODE_SETS_BY_FOLDER_ID)
    ListResponse<CodeSet> list(UUID folderId) {
        super.list(folderId)
    }

    @Get(value = Paths.CODE_SETS)
    ListResponse<CodeSet> listAll() {
        log.debug("********   CodeSetController:  listAll")
        super.listAll()
    }

    @Transactional
    @Put(value = Paths.FINALISE_CODE_SETS)
    CodeSet finalise(UUID id, @Body FinaliseData finaliseData) {
        super.finalise(id, finaliseData)
    }

    @Transactional
    @Put(value = Paths.CODE_SET_NEW_BRANCH_MODEL_VERSION)
    CodeSet createNewBranchModelVersion(UUID id, @Body @Nullable CreateNewVersionData createNewVersionData) {
        super.createNewBranchModelVersion(id, createNewVersionData)
    }

}
