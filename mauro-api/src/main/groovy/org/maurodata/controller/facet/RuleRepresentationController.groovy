package org.maurodata.controller.facet

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import org.maurodata.audit.Audit

import io.micronaut.core.annotation.Nullable
import io.micronaut.http.HttpResponse
import org.maurodata.api.Paths
import org.maurodata.api.facet.RuleRepresentationApi
import org.maurodata.controller.model.ItemController
import org.maurodata.domain.facet.Facet
import org.maurodata.domain.facet.Rule
import org.maurodata.domain.facet.RuleRepresentation
import org.maurodata.domain.model.AdministeredItem
import org.maurodata.domain.model.Item
import org.maurodata.domain.security.Role
import org.maurodata.persistence.cache.FacetCacheableRepository
import org.maurodata.persistence.cache.ItemCacheableRepository
import org.maurodata.persistence.facet.RuleRepresentationRepository
import org.maurodata.web.ListResponse

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import io.micronaut.http.HttpStatus
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Delete
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Post
import io.micronaut.http.annotation.Put
import io.micronaut.http.exceptions.HttpStatusException
import io.micronaut.security.annotation.Secured
import io.micronaut.security.rules.SecurityRule
import io.micronaut.transaction.annotation.Transactional
import jakarta.inject.Inject
import io.micronaut.core.annotation.NonNull
import org.maurodata.web.PaginationParams

@CompileStatic
@Slf4j
@Controller
@Secured(SecurityRule.IS_ANONYMOUS)
class RuleRepresentationController extends ItemController<RuleRepresentation> implements RuleRepresentationApi {

    @Inject
    RuleRepresentationRepository ruleRepresentationRepository

    @Inject
    FacetCacheableRepository.RuleCacheableRepository ruleCacheableRepository

    ItemCacheableRepository.RuleRepresentationCacheableRepository ruleRepresentationCacheableRepository

    /**
     * Properties disallowed in a simple update request.
     */
    List<String> getDisallowedProperties() {
        super.getDisallowedProperties()
    }

    RuleRepresentationController(ItemCacheableRepository.RuleRepresentationCacheableRepository ruleRepresentationCacheableRepository) {
        super(ruleRepresentationCacheableRepository)
        this.ruleRepresentationCacheableRepository = ruleRepresentationCacheableRepository
    }

    @Audit
    @Operation(operationId = 'createRuleRepresentation', summary = "Create a rule representation", description = "Creates a rule representation. You must have edit privileges on the item in question.")
    @Post(Paths.RULE_REPRESENTATIONS_LIST)
    RuleRepresentation create(@NonNull String domainType, @NonNull UUID domainId, @NonNull UUID ruleId,
                              @Body RuleRepresentation ruleRepresentation) {
        super.cleanBody(ruleRepresentation)
        Rule rule = validateAndGet(domainType, domainId, ruleId)
        accessControlService.checkRole(Role.EDITOR, readAdministeredItemForFacet(rule))
        ruleRepresentation.ruleId = rule.id
        updateCreationProperties(ruleRepresentation)
        ruleRepresentationCacheableRepository.save(ruleRepresentation)
    }

    @Audit
    @Operation(operationId = 'showRuleRepresentation', summary = "Get a rule representation", description = "Returns a rule representation. You must have read privileges on the item in question.")
    @Get(Paths.RULE_REPRESENTATIONS_ID)
    RuleRepresentation show(@NonNull String domainType, @NonNull UUID domainId, @NonNull UUID ruleId,
                            @NonNull UUID id) {
        Rule rule = validateAndGet(domainType, domainId, ruleId)
        accessControlService.checkRole(Role.READER, readAdministeredItemForFacet(rule))
        ruleRepresentationCacheableRepository.findById(id)
    }


    @Audit
    @Operation(operationId = 'listRuleRepresentationsPaged', summary = "List the rule representations", description = "Returns the rule representations. You must have read privileges on the item in question.")
    @Get(Paths.RULE_REPRESENTATIONS_LIST_PAGED)
    ListResponse<Rule> list(@NonNull String domainType, @NonNull UUID domainId, @NonNull UUID ruleId, @Nullable PaginationParams params = new PaginationParams()) {
        
        Rule rule = validateAndGet(domainType, domainId, ruleId)
        accessControlService.checkRole(Role.READER, readAdministeredItemForFacet(rule))
        List<RuleRepresentation> ruleRepresentationList = ruleRepresentationRepository.findAllByRuleId(ruleId)
        ListResponse.from(ruleRepresentationList, params)
    }

    @Audit
    @Operation(operationId = 'updateRuleRepresentation', summary = "Update a rule representation", description = "Updates a rule representation. You must have edit privileges on the item in question.")
    @Put(Paths.RULE_REPRESENTATIONS_ID)
    RuleRepresentation update(@NonNull String domainType, @NonNull UUID domainId, @NonNull UUID ruleId,
                              @NonNull UUID id, @Body @NonNull RuleRepresentation ruleRepresentation) {
        super.cleanBody(ruleRepresentation)
        RuleRepresentation existing = ruleRepresentationCacheableRepository.readById(id)
        throwNotFoundException(existing, id)
        Rule rule = validateAndGet(domainType, domainId, ruleId)
        accessControlService.checkRole(Role.EDITOR, readAdministeredItemForFacet(rule))
        RuleRepresentation updated = updateEntity(existing, ruleRepresentation, rule)
        updated
    }

    @Audit(deletedObjectDomainType = RuleRepresentation)
    @ApiResponse(responseCode = "204", description = "No content - deleted successfully")
    @Operation(operationId = 'deleteRuleRepresentation', summary = "Delete a rule representation", description = "Deletes a rule representation. You must have edit privileges on the item in question.")
    @Delete(Paths.RULE_REPRESENTATIONS_ID)
    @Transactional
    HttpResponse delete(@NonNull String domainType, @NonNull UUID domainId, @NonNull UUID ruleId,
                        @NonNull UUID id) {
        Rule rule = validateAndGet(domainType, domainId, ruleId)
        accessControlService.checkRole(Role.EDITOR, readAdministeredItemForFacet(rule))
        RuleRepresentation ruleRepresentation = ruleRepresentationCacheableRepository.readById(id)
        if (!ruleRepresentation) {
            throwNotFoundException(ruleRepresentation, id)
        }
        ruleRepresentationCacheableRepository.delete(ruleRepresentation)
        HttpResponse.status(HttpStatus.NO_CONTENT)
    }

    private RuleRepresentation updateEntity(RuleRepresentation existing, RuleRepresentation cleaned,
                                            Rule rule) {
        boolean hasChanged = updateProperties(existing, cleaned)
        if (hasChanged) {
            ruleRepresentationCacheableRepository.update(existing)
        } else {
            existing
        }
    }

    private Rule validateAndGet(String domainType, UUID domainId, UUID ruleId) {
        Rule rule = ruleCacheableRepository.readById(ruleId)
        if (!rule) {
            throwNotFoundException(null, ruleId)
        }
        if (rule.multiFacetAwareItemId != domainId || !ruleCacheableRepository.handles(domainType)) {
            throwNotFoundException(rule, ruleId)
        }
        rule
    }

    private static void throwNotFoundException(Item item, UUID id) {
        if (!item) {
            throw new HttpStatusException(HttpStatus.NOT_FOUND, "Item $id not found ")
        }
    }

    protected AdministeredItem readAdministeredItemForFacet(Facet facet) {
        readAdministeredItem(facet.multiFacetAwareItemDomainType, facet.multiFacetAwareItemId)
    }
}