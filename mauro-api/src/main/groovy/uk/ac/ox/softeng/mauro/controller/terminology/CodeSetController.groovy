package uk.ac.ox.softeng.mauro.controller.terminology

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import io.micronaut.core.annotation.NonNull
import io.micronaut.core.annotation.Nullable
import io.micronaut.http.HttpStatus
import io.micronaut.http.annotation.*
import io.micronaut.http.exceptions.HttpStatusException
import io.micronaut.security.annotation.Secured
import io.micronaut.security.rules.SecurityRule
import io.micronaut.transaction.annotation.Transactional
import jakarta.inject.Inject
import uk.ac.ox.softeng.mauro.controller.model.ModelController
import uk.ac.ox.softeng.mauro.domain.model.version.CreateNewVersionData
import uk.ac.ox.softeng.mauro.domain.model.version.FinaliseData
import uk.ac.ox.softeng.mauro.domain.security.Role
import uk.ac.ox.softeng.mauro.domain.terminology.CodeSet
import uk.ac.ox.softeng.mauro.domain.terminology.CodeSetService
import uk.ac.ox.softeng.mauro.domain.terminology.Term
import uk.ac.ox.softeng.mauro.persistence.cache.AdministeredItemCacheableRepository
import uk.ac.ox.softeng.mauro.persistence.cache.ModelCacheableRepository
import uk.ac.ox.softeng.mauro.persistence.terminology.CodeSetContentRepository
import uk.ac.ox.softeng.mauro.persistence.terminology.CodeSetRepository
import uk.ac.ox.softeng.mauro.web.ListResponse

@Controller
@CompileStatic
@Slf4j
@Secured(SecurityRule.IS_ANONYMOUS)
class CodeSetController extends ModelController<CodeSet> {

    ModelCacheableRepository.CodeSetCacheableRepository codeSetRepository

    @Inject
    CodeSetRepository codeSetRepositoryUnCached

    CodeSetContentRepository codeSetContentRepository

    @Inject
    AdministeredItemCacheableRepository.TermCacheableRepository termRepository

    CodeSetService codeSetService

    CodeSetController(ModelCacheableRepository.CodeSetCacheableRepository codeSetRepository, ModelCacheableRepository.FolderCacheableRepository folderRepository, CodeSetContentRepository codeSetContentRepository,
                      CodeSetService  codeSetService) {
        super(CodeSet, codeSetRepository, folderRepository, codeSetContentRepository ,codeSetService)
        this.codeSetRepository = codeSetRepository
        this.codeSetContentRepository = codeSetContentRepository
        this.codeSetService = codeSetService
    }

    @Get(value = Paths.CODE_SET_BY_ID)
    CodeSet show(UUID id) {
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

    @Put(value = Paths.TERM_TO_CODE_SET)
    CodeSet addTerm(@NonNull UUID id,
                    @NonNull UUID termId) {

        Term term = termRepository.readById(termId)
        accessControlService.checkRole(Role.READER, term)
        handleError(HttpStatus.NOT_FOUND, term, "Term item $termId not found")
        CodeSet codeSet = codeSetRepository.readById(id)
        accessControlService.checkRole(Role.EDITOR, codeSet)
        handleError(HttpStatus.NOT_FOUND, term, "CodeSet item $id not found")
        codeSetRepositoryUnCached.addTerm(id, termId)
        codeSet
    }

    @Transactional
    @Delete(value = Paths.CODE_SET_BY_ID)
    HttpStatus delete(UUID id, @Body @Nullable CodeSet codeSet) {
        super.delete(id, codeSet)
    }

    @Transactional
    @Delete(value = Paths.TERM_TO_CODE_SET)
    CodeSet removeTermFromCodeSet(@NonNull UUID id,
                                  @NonNull UUID termId) {
        Term term = termRepository.readById(termId)
        handleError(HttpStatus.NOT_FOUND, term, "Term item $termId not found")
        CodeSet codeSet = codeSetRepository.readById(id)
        accessControlService.checkRole(Role.EDITOR, codeSet)
        handleError(HttpStatus.NOT_FOUND, term, "CodeSet item $id not found")
        codeSetRepositoryUnCached.removeTerm(id, termId)
        codeSet
    }


    @Get(value = Paths.CODE_SETS_BY_FOLDER_ID)
    ListResponse<CodeSet> list(UUID folderId) {
        super.list(folderId)
    }

    @Get(value = Paths.CODE_SETS)
    ListResponse<CodeSet> listAll() {
        super.listAll()
    }

    @Get(value = Paths.TERMS_IN_CODE_SET)
    ListResponse<Term> listAllTermsInCodeSet(@NonNull UUID id) {
        CodeSet codeSet = codeSetRepository.readById(id)
        if (!codeSet) {
            throw new HttpStatusException(HttpStatus.NOT_FOUND, 'CodeSet item not found')
        }
        accessControlService.checkRole(Role.READER, codeSet)
        List<Term> associatedTerms = codeSetContentRepository.codeSetRepository.getTerms(id) as List<Term>
        ListResponse.from(associatedTerms)
    }

    @Transactional
    @Put(value = Paths.FINALISE_CODE_SETS)
    CodeSet finalise(UUID id, @Body FinaliseData finaliseData) {
        super.finalise(id, finaliseData)
    }

    @Transactional
    @Put(value = Paths.CODE_SET_NEW_BRANCH_MODEL_VERSION)
    CodeSet createNewBranchModelVersion(UUID id, @Body @Nullable CreateNewVersionData createNewVersionData) {
        //new branchModelVersion must point to original's terms if any
        CodeSet newBranchModelVersion = super.createNewBranchModelVersion(id, createNewVersionData) as CodeSet
        List<Term> terms = codeSetContentRepository.codeSetRepository.getTerms(id) as List<Term>
        terms.each{
            addTerm(newBranchModelVersion.id, it.id)
        }
        newBranchModelVersion
    }

}
