package org.maurodata.controller.terminology

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
    @Get(Paths.TERM_ID)
    Term show(UUID terminologyId, UUID id) {
        super.show(id)
    }

    @Audit
    @Post(Paths.TERM_LIST)
    Term create(UUID terminologyId, @Body @NonNull Term term) {
        super.create(terminologyId, term)
    }

    @Audit
    @Put(Paths.TERM_ID)
    Term update(UUID terminologyId, UUID id, @Body @NonNull Term term) {
        super.update(id, term)
    }

    @Audit(deletedObjectDomainType = Term)
    @Delete(Paths.TERM_ID)
    HttpResponse delete(UUID terminologyId, UUID id, @Body @Nullable Term term) {
        super.delete(id, term)
    }

    @Audit
    @Get(Paths.TERM_LIST_PAGED)
    ListResponse<Term> list(UUID terminologyId, @Nullable PaginationParams params = new PaginationParams()) {
        
        super.list(terminologyId, params)
    }

    @Audit
    @Get(Paths.TERM_TREE)
    List<Term> tree(UUID terminologyId, @Nullable UUID id) {
        Terminology terminology = terminologyRepository.readById(terminologyId)
        accessControlService.checkRole(Role.READER, terminology)
        termRepository.readChildTermsByParent(terminologyId, id)
    }

    @Audit
    @Get(Paths.TERM_CODE_SETS_PAGED)
    ListResponse<CodeSet> getCodeSetsForTerm(UUID terminologyId, UUID id, @Nullable PaginationParams params = new PaginationParams()) {
        
        List<CodeSet> codeSets = termRepositoryUncached.getCodeSets(id)
        codeSets = codeSets.findAll { accessControlService.canDoRole(Role.READER, it) }
        ListResponse.from(codeSets, params)
    }

    @Get(Paths.TERM_DOI)
    @Override
    Map doi(UUID id) {
        ErrorHandler.handleError(HttpStatus.UNPROCESSABLE_ENTITY, "Doi is not implemented")
        return null
    }
}