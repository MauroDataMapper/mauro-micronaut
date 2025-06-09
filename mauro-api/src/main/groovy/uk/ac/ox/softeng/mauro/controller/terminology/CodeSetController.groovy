package uk.ac.ox.softeng.mauro.controller.terminology

import uk.ac.ox.softeng.mauro.ErrorHandler
import uk.ac.ox.softeng.mauro.api.Paths
import uk.ac.ox.softeng.mauro.api.model.PermissionsDTO
import uk.ac.ox.softeng.mauro.api.terminology.CodeSetApi
import uk.ac.ox.softeng.mauro.audit.Audit
import uk.ac.ox.softeng.mauro.controller.model.ModelController
import uk.ac.ox.softeng.mauro.domain.diff.ObjectDiff
import uk.ac.ox.softeng.mauro.domain.facet.EditType
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

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import io.micronaut.core.annotation.NonNull
import io.micronaut.core.annotation.Nullable
import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpStatus
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Delete
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Post
import io.micronaut.http.annotation.Put
import io.micronaut.http.annotation.QueryValue
import io.micronaut.http.exceptions.HttpStatusException
import io.micronaut.security.annotation.Secured
import io.micronaut.security.rules.SecurityRule
import io.micronaut.transaction.annotation.Transactional
import jakarta.inject.Inject

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

    @Audit
    @Get(value = Paths.CODE_SET_ID)
    CodeSet show(UUID id) {
        super.show(id)
    }

    @Audit
    @Transactional
    @Post(value = Paths.FOLDER_LIST_CODE_SET)
    CodeSet create(UUID folderId, @Body @NonNull CodeSet codeSet) {
        super.create(folderId, codeSet)
    }

    @Audit
    @Put(value = Paths.CODE_SET_ID)
    CodeSet update(UUID id, @Body @NonNull CodeSet codeSet) {
        super.update(id, codeSet)
    }

    @Audit(title = EditType.UPDATE, description = "Add term to CodeSet")
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

    @Audit
    @Transactional
    @Delete(value = Paths.CODE_SET_ID)
    HttpResponse delete(UUID id, @Body @Nullable CodeSet codeSet, @Nullable @QueryValue Boolean permanent) {
        permanent = permanent ?: true
        super.delete(id, codeSet,permanent)
    }

    @Transactional
    @Audit(title = EditType.UPDATE, description = "Remove term from CodeSet")
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


    @Audit
    @Get(value = Paths.FOLDER_LIST_CODE_SET)
    ListResponse<CodeSet> list(UUID folderId) {
        super.list(folderId)
    }

    @Audit
    @Get(value = Paths.CODE_SET_LIST)
    ListResponse<CodeSet> listAll() {
        super.listAll()
    }

    @Audit
    @Get(value = Paths.CODE_SET_TERM_LIST)
    ListResponse<Term> listAllTermsInCodeSet(@NonNull UUID id) {
        CodeSet codeSet = codeSetRepository.readById(id)
        if (!codeSet) {
            throw new HttpStatusException(HttpStatus.NOT_FOUND, 'CodeSet item not found')
        }
        accessControlService.checkRole(Role.READER, codeSet)
        List<Term> associatedTerms = codeSetContentRepository.codeSetRepository.getTerms(id).each{it.updateBreadcrumbs()} as List<Term>
        ListResponse.from(associatedTerms)
    }

    @Transactional
    @Audit(title = EditType.FINALISE, description = "Finalise CodeSet")
    @Put(value = Paths.CODE_SET_FINALISE)
    CodeSet finalise(UUID id, @Body FinaliseData finaliseData) {
        super.finalise(id, finaliseData)
    }


    @Audit
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
    @Audit(title = EditType.COPY, description = "New Version of CodeSet")
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

    //stub endpoint todo: actual
    @Get('/codeSets/{id}/simpleModelVersionTree')
    List<Map> simpleModelVersionTree(UUID id){
        super.simpleModelVersionTree(id)
    }

    @Audit
    @Put(Paths.CODE_SET_READ_BY_EVERYONE)
    @Transactional
    CodeSet allowReadByEveryone(UUID id) {
        super.putReadByEveryone(id) as CodeSet
    }

    @Audit
    @Transactional
    @Delete(Paths.CODE_SET_READ_BY_EVERYONE)
    HttpResponse revokeReadByEveryone(UUID id) {
        super.deleteReadByEveryone(id)
    }

    @Audit
    @Put(Paths.CODE_SET_READ_BY_AUTHENTICATED)
    @Transactional
    CodeSet allowReadByAuthenticated(UUID id) {
        super.putReadByAuthenticated(id) as CodeSet
    }

    @Audit
    @Transactional
    @Delete(Paths.CODE_SET_READ_BY_AUTHENTICATED)
    HttpResponse revokeReadByAuthenticated(UUID id) {
        super.deleteReadByAuthenticated(id)
    }

    @Audit
    @Get(Paths.CODE_SET_FOLDER_PERMISSIONS)
    @Override
    PermissionsDTO permissions(UUID id) {
        super.permissions(id)
    }

    @Audit
    @Get(Paths.CODE_SET_DOI)
    @Override
    Map doi(UUID id) {
        ErrorHandler.handleError(HttpStatus.UNPROCESSABLE_ENTITY,"Doi is not implemented")
        return null
    }
}
