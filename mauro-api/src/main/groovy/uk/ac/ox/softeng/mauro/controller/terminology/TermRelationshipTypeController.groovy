package uk.ac.ox.softeng.mauro.controller.terminology

import uk.ac.ox.softeng.mauro.api.Paths
import uk.ac.ox.softeng.mauro.api.terminology.TermRelationshipTypeApi
import uk.ac.ox.softeng.mauro.audit.Audit
import uk.ac.ox.softeng.mauro.domain.terminology.TermRelationship
import uk.ac.ox.softeng.mauro.web.PaginationParams

import groovy.transform.CompileStatic
import io.micronaut.core.annotation.NonNull
import io.micronaut.core.annotation.Nullable
import io.micronaut.http.HttpResponse
import io.micronaut.http.annotation.*
import io.micronaut.security.annotation.Secured
import io.micronaut.security.rules.SecurityRule
import uk.ac.ox.softeng.mauro.controller.model.AdministeredItemController
import uk.ac.ox.softeng.mauro.domain.terminology.TermRelationshipType
import uk.ac.ox.softeng.mauro.domain.terminology.Terminology
import uk.ac.ox.softeng.mauro.persistence.cache.AdministeredItemCacheableRepository.TermRelationshipTypeCacheableRepository
import uk.ac.ox.softeng.mauro.persistence.cache.ModelCacheableRepository.TerminologyCacheableRepository
import uk.ac.ox.softeng.mauro.persistence.terminology.TermRelationshipTypeContentRepository
import uk.ac.ox.softeng.mauro.web.ListResponse

@CompileStatic
@Controller
@Secured(SecurityRule.IS_ANONYMOUS)
class TermRelationshipTypeController extends AdministeredItemController<TermRelationshipType, Terminology> implements TermRelationshipTypeApi {

    TermRelationshipTypeController(TermRelationshipTypeCacheableRepository termRelationshipTypeRepository, TerminologyCacheableRepository terminologyRepository,
                                   TermRelationshipTypeContentRepository termRelationshipTypeContentRepository) {
        super(TermRelationshipType, termRelationshipTypeRepository, terminologyRepository, termRelationshipTypeContentRepository)
    }

    @Audit
    @Get(Paths.TERM_RELATIONSHIP_TYPE_ID)
    TermRelationshipType show(UUID terminologyId, UUID id) {
        super.show(id)
    }

    @Audit
    @Post(Paths.TERM_RELATIONSHIP_TYPE_LIST)
    TermRelationshipType create(UUID terminologyId, @Body @NonNull TermRelationshipType termRelationshipType) {
        super.create(terminologyId, termRelationshipType)
    }

    @Audit
    @Put(Paths.TERM_RELATIONSHIP_TYPE_ID)
    TermRelationshipType update(UUID terminologyId, UUID id, @Body @NonNull TermRelationshipType termRelationshipType) {
        super.update(id, termRelationshipType)
    }

    @Audit
    @Get(Paths.TERM_RELATIONSHIP_TYPE_LIST_PAGED)
    ListResponse<TermRelationshipType> list(UUID terminologyId, @Nullable PaginationParams params = new PaginationParams()) {
        super.list(terminologyId, params)
    }

    @Audit(deletedObjectDomainType = TermRelationshipType, parentDomainType = Terminology, parentIdParamName = "terminologyId")
    @Delete(Paths.TERM_RELATIONSHIP_TYPE_ID)
    HttpResponse delete(UUID terminologyId, UUID id, @Body @Nullable TermRelationshipType termRelationshipType) {
        super.delete(id, termRelationshipType)
    }
}
