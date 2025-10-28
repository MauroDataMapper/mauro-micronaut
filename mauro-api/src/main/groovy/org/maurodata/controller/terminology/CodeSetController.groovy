package org.maurodata.controller.terminology

import io.micronaut.http.MediaType
import io.micronaut.http.annotation.Consumes
import io.micronaut.http.server.multipart.MultipartBody
import io.micronaut.scheduling.TaskExecutors
import io.micronaut.scheduling.annotation.ExecuteOn
import org.maurodata.ErrorHandler
import org.maurodata.api.Paths
import org.maurodata.api.model.ModelVersionedRefDTO
import org.maurodata.api.model.PermissionsDTO
import org.maurodata.api.terminology.CodeSetApi
import org.maurodata.audit.Audit
import org.maurodata.controller.model.ModelController
import org.maurodata.domain.diff.ObjectDiff
import org.maurodata.domain.facet.EditType
import org.maurodata.domain.model.version.CreateNewVersionData
import org.maurodata.domain.model.version.FinaliseData
import org.maurodata.domain.security.Role
import org.maurodata.domain.terminology.CodeSet
import org.maurodata.domain.terminology.CodeSetService
import org.maurodata.domain.terminology.Term
import org.maurodata.persistence.cache.AdministeredItemCacheableRepository
import org.maurodata.persistence.cache.ModelCacheableRepository
import org.maurodata.persistence.terminology.CodeSetContentRepository
import org.maurodata.persistence.terminology.CodeSetRepository
import org.maurodata.web.ListResponse
import org.maurodata.web.PaginationParams

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

    CodeSetController(ModelCacheableRepository.CodeSetCacheableRepository codeSetRepository, ModelCacheableRepository.FolderCacheableRepository folderRepository,
                      CodeSetContentRepository codeSetContentRepository,
                      CodeSetService codeSetService) {
        super(CodeSet, codeSetRepository, folderRepository, codeSetContentRepository, codeSetService)
        this.codeSetRepository = codeSetRepository
        this.codeSetContentRepository = codeSetContentRepository
        this.codeSetService = codeSetService
    }

    @Audit
    @Get('/api/codeSets/undefined')
    Map showUndef() {
        [:]
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
        super.delete(id, codeSet, permanent)
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
    @Get(value = Paths.CODE_SET_LIST_PAGED)
    ListResponse<CodeSet> listAll(@Nullable PaginationParams params = new PaginationParams()) {
        
        super.listAll(params)
    }

    @Audit
    @Get(value = Paths.CODE_SET_TERM_LIST_PAGED)
    ListResponse<Term> listAllTermsInCodeSet(@NonNull UUID id, @Nullable PaginationParams params = new PaginationParams()) {
        
        CodeSet codeSet = codeSetRepository.readById(id)
        if (!codeSet) {
            throw new HttpStatusException(HttpStatus.NOT_FOUND, 'CodeSet item not found')
        }
        accessControlService.checkRole(Role.READER, codeSet)
        List<Term> associatedTerms = codeSetContentRepository.codeSetRepository.getTerms(id).each { it.updateBreadcrumbs() } as List<Term>
        ListResponse.from(associatedTerms, params)
    }

    @Transactional
    @Audit(title = EditType.FINALISE, description = "Finalise CodeSet")
    @Put(value = Paths.CODE_SET_FINALISE)
    CodeSet finalise(UUID id, @Body FinaliseData finaliseData) {
        super.finalise(id, finaliseData)
    }


    @Audit
    @Get(Paths.CODE_SET_DIFF)
    ObjectDiff diffModels(@NonNull UUID id, @NonNull UUID otherId) {
        CodeSet codeSet = modelContentRepository.findWithContentById(id)
        ErrorHandler.handleErrorOnNullObject(HttpStatus.NOT_FOUND, codeSet, "item not found : $id")
        CodeSet other = modelContentRepository.findWithContentById(otherId)
        ErrorHandler.handleErrorOnNullObject(HttpStatus.NOT_FOUND, codeSet, "item not found : $otherId")

        accessControlService.checkRole(Role.READER, codeSet)
        accessControlService.checkRole(Role.READER, other)

        codeSet.setAssociations()
        other.setAssociations()

        pathRepository.readParentItems(codeSet)
        codeSet.updatePath()

        pathRepository.readParentItems(other)
        other.updatePath()

        codeSet.diff(other)
    }

    @Transactional
    @Audit(title = EditType.COPY, description = "New Version of CodeSet")
    @Put(value = Paths.CODE_SET_NEW_BRANCH_MODEL_VERSION)
    CodeSet createNewBranchModelVersion(UUID id, @Body @Nullable CreateNewVersionData createNewVersionData) {
        if (!createNewVersionData) createNewVersionData = new CreateNewVersionData()
        CodeSet existing = super.getExistingWithContent(id) as CodeSet

        CodeSet copy = createCopyModelWithAssociations(existing, createNewVersionData)
        copy.setAssociations()
        copy.terms.clear()
        CodeSet savedCopy = (CodeSet) contentsService.saveWithContent(copy)
        List<Term> terms = codeSetContentRepository.codeSetRepository.getTerms(id) as List<Term>
        terms.each {
            addTerm(savedCopy.id, it.id)
        }
        savedCopy
    }

    @Override
    @Audit(title = EditType.IMPORT, description = "Import codeSet")
    @Transactional
    @ExecuteOn(TaskExecutors.IO)
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Post(Paths.CODE_SET_IMPORT)
    ListResponse<CodeSet> importModel(@Body MultipartBody body, String namespace, String name, @Nullable String version) {
        super.importModel(body, namespace, name, version)

    }

    //stub endpoint todo: actual
    @Get(Paths.CODE_SET_SIMPLE_MODEL_VERSION_TREE)
    List<ModelVersionedRefDTO> simpleModelVersionTree(UUID id, @Nullable Boolean branchesOnly) {
        super.simpleModelVersionTree(id,branchesOnly)
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
        ErrorHandler.handleError(HttpStatus.UNPROCESSABLE_ENTITY, "Doi is not implemented")
        return null
    }
}
