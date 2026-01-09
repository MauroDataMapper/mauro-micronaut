package org.maurodata.controller.terminology

import org.maurodata.api.Paths
import org.maurodata.api.terminology.TermRelationshipTypeApi
import org.maurodata.audit.Audit
import org.maurodata.web.PaginationParams

import groovy.transform.CompileStatic
import io.micronaut.core.annotation.NonNull
import io.micronaut.core.annotation.Nullable
import io.micronaut.http.HttpResponse
import io.micronaut.http.annotation.*
import io.micronaut.security.annotation.Secured
import io.micronaut.security.rules.SecurityRule
import org.maurodata.controller.model.AdministeredItemController
import org.maurodata.domain.terminology.TermRelationshipType
import org.maurodata.domain.terminology.Terminology
import org.maurodata.persistence.cache.AdministeredItemCacheableRepository.TermRelationshipTypeCacheableRepository
import org.maurodata.persistence.cache.ModelCacheableRepository.TerminologyCacheableRepository

import org.maurodata.web.ListResponse

@CompileStatic
@Controller
@Secured(SecurityRule.IS_ANONYMOUS)
class TermRelationshipTypeController extends AdministeredItemController<TermRelationshipType, Terminology> implements TermRelationshipTypeApi {

    TermRelationshipTypeController(TermRelationshipTypeCacheableRepository termRelationshipTypeRepository, TerminologyCacheableRepository terminologyRepository) {
        super(TermRelationshipType, termRelationshipTypeRepository, terminologyRepository)
    }

    @Audit
    @Get(Paths.TERM_RELATIONSHIP_TYPE_ID)
    TermRelationshipType show(UUID terminologyId, UUID id) {
        super.show(id)
    }

    @Audit
    @Post(Paths.TERM_RELATIONSHIP_TYPE_LIST)
    TermRelationshipType create(UUID terminologyId, @Body @NonNull TermRelationshipType termRelationshipType) {
        termRelationshipType.displayLabel = termRelationshipType.createDisplayLabel()
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
