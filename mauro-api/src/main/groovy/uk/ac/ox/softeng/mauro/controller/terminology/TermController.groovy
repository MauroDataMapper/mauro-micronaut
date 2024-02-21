package uk.ac.ox.softeng.mauro.controller.terminology

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import io.micronaut.core.annotation.NonNull
import io.micronaut.core.annotation.Nullable
import io.micronaut.http.HttpStatus
import io.micronaut.http.annotation.*
import jakarta.inject.Inject
import uk.ac.ox.softeng.mauro.domain.terminology.CodeSet
import uk.ac.ox.softeng.mauro.controller.model.AdministeredItemController
import uk.ac.ox.softeng.mauro.domain.terminology.Term
import uk.ac.ox.softeng.mauro.domain.terminology.Terminology
import uk.ac.ox.softeng.mauro.persistence.terminology.TermContentRepository
import uk.ac.ox.softeng.mauro.persistence.terminology.TermRepository
import uk.ac.ox.softeng.mauro.persistence.cache.AdministeredItemCacheableRepository.TermCacheableRepository
import uk.ac.ox.softeng.mauro.persistence.cache.ModelCacheableRepository.TerminologyCacheableRepository
import uk.ac.ox.softeng.mauro.web.ListResponse

@CompileStatic
@Controller('/terminologies/{terminologyId}/terms')
@Slf4j
class TermController extends AdministeredItemController<Term, Terminology> {

    TermCacheableRepository termRepository
    @Inject
    TermRepository termRepo

    TermController(TermCacheableRepository termRepository, TerminologyCacheableRepository terminologyRepository, TermContentRepository termContentRepository) {
        super(Term, termRepository, terminologyRepository, termContentRepository)
        this.termRepository = termRepository
    }

    @Get('/{id}')
    Term show(UUID terminologyId, UUID id) {
        super.show(id)
    }

    @Post
    Term create(UUID terminologyId, @Body @NonNull Term term) {
        super.create(terminologyId, term)
    }

    @Put('/{id}')
    Term update(UUID terminologyId, UUID id, @Body @NonNull Term term) {
        super.update(id, term)
    }

    @Delete('/{id}')
    HttpStatus delete(UUID terminologyId, UUID id, @Body @Nullable Term term) {
        super.delete(id, term)
    }

    @Get
    ListResponse<Term> list(UUID terminologyId) {
        log.debug '** start list **'
        ListResponse<Term> listResponse = super.list(terminologyId)
        log.debug '** end list **'
        listResponse
    }

    @Get("/tree{/id}")
    List<Term> tree(UUID terminologyId, @Nullable UUID id) {
        termRepository.readChildTermsByParent(terminologyId, id)
    }

    @Get('/{id}/codeSets')
    ListResponse<CodeSet> getCodeSetsForTerm(UUID id) {
        List<CodeSet> codeSets = termRepo.getCodeSets(id)
        ListResponse.from(codeSets)
    }
}