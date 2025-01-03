package uk.ac.ox.softeng.mauro.controller.terminology

import uk.ac.ox.softeng.mauro.api.Paths
import uk.ac.ox.softeng.mauro.api.terminology.CodeSetApi
import uk.ac.ox.softeng.mauro.ErrorHandler

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
import uk.ac.ox.softeng.mauro.Paths
import uk.ac.ox.softeng.mauro.controller.model.ModelController
import uk.ac.ox.softeng.mauro.domain.diff.ObjectDiff
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
class CodeSetController extends ModelController<CodeSet> implements CodeSetApi {

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

    @Get(value = Paths.CODE_SET_ID)
    CodeSet show(UUID id) {
        super.show(id)
    }

    @Transactional
    @Post(value = Paths.FOLDER_LIST_CODE_SET)
    CodeSet create(UUID folderId, @Body @NonNull CodeSet codeSet) {
        super.create(folderId, codeSet)
    }

    @Put(value = Paths.CODE_SET_ID)
    CodeSet update(UUID id, @Body @NonNull CodeSet codeSet) {
        super.update(id, codeSet)
    }

    @Put(value = Paths.CODE_SET_TERM_ID)
    @Transactional
    CodeSet addTerm(@NonNull UUID id,
                    @NonNull UUID termId) {

        Term term = termRepository.readById(termId)
        accessControlService.checkRole(Role.READER, term)
        ErrorHandler.handleErrorOnNullObject(HttpStatus.NOT_FOUND, term, "Term item $termId not found")
        CodeSet codeSet = codeSetRepository.readById(id)
        accessControlService.checkRole(Role.EDITOR, codeSet)
        ErrorHandler.handleErrorOnNullObject(HttpStatus.NOT_FOUND, term, "CodeSet item $id not found")
        codeSetRepositoryUnCached.addTerm(id, termId)
        codeSet
    }

    @Transactional
    @Delete(value = Paths.CODE_SET_ID)
    HttpStatus delete(UUID id, @Body @Nullable CodeSet codeSet) {
        super.delete(id, codeSet)
    }

    @Transactional
    @Delete(value = Paths.CODE_SET_TERM_ID)
    CodeSet removeTermFromCodeSet(@NonNull UUID id,
                                  @NonNull UUID termId) {
        Term term = termRepository.readById(termId)
        ErrorHandler.handleErrorOnNullObject(HttpStatus.NOT_FOUND, term, "Term item $termId not found")
        CodeSet codeSet = codeSetRepository.readById(id)
        accessControlService.checkRole(Role.EDITOR, codeSet)
        ErrorHandler.handleErrorOnNullObject(HttpStatus.NOT_FOUND, term, "CodeSet item $id not found")
        codeSetRepositoryUnCached.removeTerm(id, termId)
        codeSet
    }


    @Get(value = Paths.FOLDER_LIST_CODE_SET)
    ListResponse<CodeSet> list(UUID folderId) {
        super.list(folderId)
    }

    @Get(value = Paths.CODE_SET_LIST)
    ListResponse<CodeSet> listAll() {
        super.listAll()
    }

    @Get(value = Paths.CODE_SET_TERM_LIST)
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
    @Put(value = Paths.CODE_SET_FINALISE)
    CodeSet finalise(UUID id, @Body FinaliseData finaliseData) {
        super.finalise(id, finaliseData)
    }


    @Get('/codeSets/{id}/diff/{otherId}')
    ObjectDiff diffModels(@NonNull UUID id, @NonNull UUID otherId) {
        CodeSet codeSet = modelContentRepository.findWithContentById(id)
        ErrorHandler.handleErrorOnNullObject(HttpStatus.NOT_FOUND, codeSet, "item not found : $id")
        CodeSet other = modelContentRepository.findWithContentById(otherId)
        ErrorHandler.handleErrorOnNullObject(HttpStatus.NOT_FOUND, codeSet, "item not found : $otherId")

        accessControlService.checkRole(Role.READER, codeSet)
        accessControlService.checkRole(Role.READER, other)

        codeSet.setAssociations()
        other.setAssociations()
        codeSet.diff(other)
    }

    @Transactional
    @Put(value = Paths.CODE_SET_NEW_BRANCH_MODEL_VERSION)
    CodeSet createNewBranchModelVersion(UUID id, @Body @Nullable CreateNewVersionData createNewVersionData) {
        if (!createNewVersionData) createNewVersionData = new CreateNewVersionData()
        CodeSet existing = super.getExistingWithContent(id) as CodeSet

        CodeSet copy = createCopyModelWithAssociations(existing, createNewVersionData)
        copy.terms.clear()
        CodeSet savedCopy = modelContentRepository.saveWithContent(copy)
        List<Term> terms = codeSetContentRepository.codeSetRepository.getTerms(id) as List<Term>
        terms.each{
            addTerm(savedCopy.id, it.id)
        }
        savedCopy
    }

}
