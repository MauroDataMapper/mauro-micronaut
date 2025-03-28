package uk.ac.ox.softeng.mauro.controller.terminology

import uk.ac.ox.softeng.mauro.api.Paths
import uk.ac.ox.softeng.mauro.api.terminology.TermApi

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import io.micronaut.core.annotation.NonNull
import io.micronaut.core.annotation.Nullable
import io.micronaut.http.HttpResponse
import io.micronaut.http.annotation.*
import io.micronaut.security.annotation.Secured
import io.micronaut.security.rules.SecurityRule
import jakarta.inject.Inject
import uk.ac.ox.softeng.mauro.controller.model.AdministeredItemController
import uk.ac.ox.softeng.mauro.domain.security.Role
import uk.ac.ox.softeng.mauro.domain.terminology.CodeSet
import uk.ac.ox.softeng.mauro.domain.terminology.Term
import uk.ac.ox.softeng.mauro.domain.terminology.Terminology
import uk.ac.ox.softeng.mauro.persistence.cache.AdministeredItemCacheableRepository.TermCacheableRepository
import uk.ac.ox.softeng.mauro.persistence.cache.ModelCacheableRepository.TerminologyCacheableRepository
import uk.ac.ox.softeng.mauro.persistence.terminology.TermContentRepository
import uk.ac.ox.softeng.mauro.persistence.terminology.TermRepository
import uk.ac.ox.softeng.mauro.web.ListResponse

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

    TermController(TermCacheableRepository termRepository, TerminologyCacheableRepository terminologyRepository, TermContentRepository termContentRepository) {
        super(Term, termRepository, terminologyRepository, termContentRepository)
        this.termRepository = termRepository
    }

    @Get(Paths.TERM_ID)
    Term show(UUID terminologyId, UUID id) {
        super.show(id)
    }

    @Post(Paths.TERM_LIST)
    Term create(UUID terminologyId, @Body @NonNull Term term) {
        super.create(terminologyId, term)
    }

    @Put(Paths.TERM_ID)
    Term update(UUID terminologyId, UUID id, @Body @NonNull Term term) {
        super.update(id, term)
    }

    @Delete(Paths.TERM_ID)
    HttpResponse delete(UUID terminologyId, UUID id, @Body @Nullable Term term) {
        super.delete(id, term)
    }

    @Get(Paths.TERM_LIST)
    ListResponse<Term> list(UUID terminologyId) {
        super.list(terminologyId)
    }

    @Get(Paths.TERM_TREE)
    List<Term> tree(UUID terminologyId, @Nullable UUID id) {
        Terminology terminology = terminologyRepository.readById(terminologyId)
        accessControlService.checkRole(Role.READER, terminology)
        termRepository.readChildTermsByParent(terminologyId, id)
    }

    @Get(Paths.TERM_CODE_SETS)
    ListResponse<CodeSet> getCodeSetsForTerm(UUID terminologyId, UUID id) {
        List<CodeSet> codeSets = termRepositoryUncached.getCodeSets(id)
        codeSets = codeSets.findAll {accessControlService.canDoRole(Role.READER, it)}
        ListResponse.from(codeSets)
    }


}