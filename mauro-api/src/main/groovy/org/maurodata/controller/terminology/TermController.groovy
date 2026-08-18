package org.maurodata.controller.terminology

import org.maurodata.api.terminology.TermCopyDTO
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import org.maurodata.web.PaginationParams

import io.micronaut.http.HttpStatus
import org.maurodata.ErrorHandler
import org.maurodata.api.Paths
import org.maurodata.api.terminology.TermApi
import org.maurodata.audit.Audit

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import io.micronaut.core.annotation.NonNull
import io.micronaut.core.annotation.Nullable
import io.micronaut.http.HttpResponse
import io.micronaut.http.annotation.*
import io.micronaut.security.annotation.Secured
import io.micronaut.security.rules.SecurityRule
import jakarta.inject.Inject
import org.maurodata.controller.model.AdministeredItemController
import org.maurodata.domain.security.Role
import org.maurodata.domain.terminology.CodeSet
import org.maurodata.domain.terminology.Term
import org.maurodata.domain.terminology.Terminology
import org.maurodata.persistence.cache.AdministeredItemCacheableRepository.TermCacheableRepository
import org.maurodata.persistence.cache.ModelCacheableRepository.TerminologyCacheableRepository

import org.maurodata.persistence.terminology.TermRepository
import org.maurodata.web.ListResponse

@CompileStatic
@Controller
@Slf4j
@Secured(SecurityRule.IS_ANONYMOUS)
class TermController extends AdministeredItemController<Term, Terminology> implements TermApi {

    TermCacheableRepository termRepository

    @Inject
    TermRepository termRepositoryUncached

    @Inject
    TerminologyCacheableRepository terminologyRepository

    TermController(TermCacheableRepository termRepository, TerminologyCacheableRepository terminologyRepository) {
        super(Term, termRepository, terminologyRepository)
        this.termRepository = termRepository
    }

    @Audit
    @Operation(operationId = 'showTerm', summary = "Get a term", description = "Returns a term.")
    @Get(Paths.TERM_ID)
    Term show(UUID terminologyId, UUID id) {
        super.show(id)
    }

    @Audit
    @Operation(operationId = 'createTerm', summary = "Create a term", description = "Creates a term.")
    @Post(Paths.TERM_LIST)
    Term create(UUID terminologyId, @Body @NonNull Term term) {
        super.create(terminologyId, term)
    }

    @Audit
    @Operation(operationId = 'updateTerm', summary = "Update a term", description = "Updates a term.")
    @Put(Paths.TERM_ID)
    Term update(UUID terminologyId, UUID id, @Body @NonNull Term term) {
        super.update(id, term)
    }

    @Audit(deletedObjectDomainType = Term)
    @ApiResponse(responseCode = "204", description = "No content - deleted successfully")
    @Operation(operationId = 'deleteTerm', summary = "Delete a term", description = "Deletes a term.")
    @Delete(Paths.TERM_ID)
    HttpResponse delete(UUID terminologyId, UUID id, @Body @Nullable Term term) {
        super.delete(id, term)
    }

    @Audit
    @Operation(operationId = 'listTermPaged', summary = "List the terms", description = "Returns the terms.")
    @Get(Paths.TERM_LIST_PAGED)
    ListResponse<Term> list(UUID terminologyId, @Nullable PaginationParams params = new PaginationParams()) {
        
        super.list(terminologyId, params)
    }

    @Audit
    @Operation(summary = "List the terms", description = "Returns the terms. You must have read privileges on the item in question.")
    @Get(Paths.TERM_TREE)
    List<Term> tree(UUID terminologyId, @Nullable UUID id) {
        Terminology terminology = terminologyRepository.readById(terminologyId)
        accessControlService.checkRole(Role.READER, terminology)
        termRepository.readChildTermsByParent(terminologyId, id)
    }

    @Audit
    @Operation(summary = "List the terms", description = "Returns the terms. You must have read privileges on the item in question.")
    @Get(Paths.TERM_CODE_SETS_PAGED)
    ListResponse<CodeSet> getCodeSetsForTerm(UUID terminologyId, UUID id, @Nullable PaginationParams params = new PaginationParams()) {
        
        List<CodeSet> codeSets = termRepositoryUncached.getCodeSets(id)
        codeSets = codeSets.findAll { accessControlService.canDoRole(Role.READER, it) }
        ListResponse.from(codeSets, params)
    }

    @Operation(summary = "Get a term", description = "Returns a term.")
    @Get(Paths.TERM_DOI)
    @Override
    Map doi(UUID id) {
        ErrorHandler.handleError(HttpStatus.UNPROCESSABLE_ENTITY, "Doi is not implemented")
        return null
    }

    // TODO: Replace this with something more like copying data elements
    @Audit
    @Override
    @Put(Paths.TERM_COPY)
    Term copyTerm(UUID terminologyId, UUID termId, @Body TermCopyDTO termCopyDTO) {
        Terminology terminology = terminologyRepository.readById(terminologyId)
        accessControlService.checkRole(Role.READER, terminology)
        Terminology targetTerminology = terminologyRepository.readById(termCopyDTO.targetTerminologyId)
        accessControlService.checkRole(Role.EDITOR, targetTerminology)
        Term term = termRepository.findById(termId)
        if (term.terminology.id != terminology.id ) {
            ErrorHandler.handleError(HttpStatus.BAD_REQUEST, "Term with id ${term.id} is not associated with terminology: ${terminologyId}")
        }
        if (terminologyId == termCopyDTO.targetTerminologyId && (term.code == termCopyDTO.code || !termCopyDTO.code) ) {
            ErrorHandler.handleError(HttpStatus.BAD_REQUEST, "Cannot copy into the same terminology because the code is unchanged")
        }
        term = (Term) contentsService.loadWithContent(term)
        Term newTerm = (Term) term.deepClone()
        newTerm.terminology = targetTerminology
        if(termCopyDTO.code && !termCopyDTO.code.isBlank()) {
            newTerm.code = termCopyDTO.code
            if(term.code == term.definition) {
                newTerm.definition = newTerm.code
            }

        }

        return (Term) contentsService.saveWithContent(newTerm, accessControlService.user, true)
    }

}