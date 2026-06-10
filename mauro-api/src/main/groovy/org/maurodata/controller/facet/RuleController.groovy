package org.maurodata.controller.facet

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import org.maurodata.audit.Audit

import io.micronaut.core.annotation.Nullable
import io.micronaut.http.HttpResponse
import org.maurodata.api.Paths
import org.maurodata.api.facet.RuleApi
import org.maurodata.domain.facet.Rule
import org.maurodata.domain.facet.RuleRepresentation
import org.maurodata.domain.model.AdministeredItem
import org.maurodata.domain.security.Role
import org.maurodata.persistence.cache.FacetCacheableRepository
import org.maurodata.persistence.cache.ItemCacheableRepository
import org.maurodata.persistence.facet.RuleRepresentationRepository
import org.maurodata.web.ListResponse

import groovy.transform.CompileStatic
import io.micronaut.core.annotation.NonNull
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Delete
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Post
import io.micronaut.http.annotation.Put
import io.micronaut.security.annotation.Secured
import io.micronaut.security.rules.SecurityRule
import io.micronaut.transaction.annotation.Transactional
import jakarta.inject.Inject
import org.maurodata.web.PaginationParams

@CompileStatic
@Controller
@Secured(SecurityRule.IS_ANONYMOUS)
class RuleController extends FacetController<Rule> implements RuleApi {
    FacetCacheableRepository.RuleCacheableRepository ruleRepository

    @Inject
    RuleRepresentationRepository ruleRepresentationRepositoryUncached

    @Inject
    ItemCacheableRepository.RuleRepresentationCacheableRepository ruleRepresentationCacheableRepository

    /**
     * Properties disallowed in a simple update request.
     */
    List<String> getDisallowedProperties() {
        super.getDisallowedProperties() + ['multiFacetAwareItemDomainType', 'multiFacetAwareItemId']
    }

    RuleController(FacetCacheableRepository.RuleCacheableRepository ruleRepository) {
        super(ruleRepository)
        this.ruleRepository = ruleRepository
    }

    @Audit
    @Operation(operationId = 'listRulePaged', summary = "List the rules", description = "Returns the rules. You must have read privileges on the item in question.")
    @Get(Paths.RULE_LIST_PAGED)
    ListResponse<Rule> list(String domainType, UUID domainId, @Nullable PaginationParams params = new PaginationParams()) {
        
        AdministeredItem administeredItem = findAdministeredItem(domainType, domainId)
        accessControlService.checkRole(Role.READER, administeredItem)
        ListResponse.from(!administeredItem.rules ? [] : administeredItem.rules, params)
    }

    @Audit
    @Operation(operationId = 'showRule', summary = "Get a rule", description = "Returns a rule. You must have read privileges on the item in question.")
    @Get(Paths.RULE_ID)
    Rule show(@NonNull String domainType, @NonNull UUID domainId, UUID id) {
        Rule rule = super.show(domainType, domainId, id) as Rule
        if (rule) {
            AdministeredItem administeredItem = findAdministeredItem(rule.multiFacetAwareItemDomainType,
                    rule.multiFacetAwareItemId)
            accessControlService.checkRole(Role.READER, administeredItem)
            List<Rule> ruleList = administeredItem.rules
            Rule ruleWithReports = ruleList.find { it -> it.id == id }
            ruleWithReports
        }
    }

    @Audit
    @Operation(operationId = 'createRule', summary = "Create a rule", description = "Creates a rule.")
    @Post(Paths.RULE_LIST)
    Rule create(@NonNull String domainType, @NonNull UUID domainId, @Body @NonNull Rule rule) {
        super.create(domainType, domainId, rule)
    }

    @Audit
    @Operation(operationId = 'updateRule', summary = "Update a rule", description = "Updates a rule.")
    @Put(Paths.RULE_ID)
    Rule update(@NonNull String domainType, @NonNull UUID domainId, UUID id, @Body @NonNull Rule rule) {
        super.update(id, rule)
    }

    @Audit(deletedObjectDomainType = Rule)
    @ApiResponse(responseCode = "204", description = "No content - deleted successfully")
    @Operation(operationId = 'deleteRule', summary = "Delete a rule", description = "Deletes a rule. You must have read privileges on the item in question.")
    @Delete(Paths.RULE_ID)
    @Transactional
    @Override
    HttpResponse delete(@NonNull String domainType, @NonNull UUID domainId, UUID id) {
        accessControlService.checkRole(Role.READER, readAdministeredItemForFacet(ruleRepository.readById(id)))
        deleteAnyAssociatedRepresentations(id)
        super.delete(id)
    }

    private void deleteAnyAssociatedRepresentations(UUID ruleId) {
        List<RuleRepresentation> savedRuleRepresentations = ruleRepresentationRepositoryUncached.findAllByRuleId(ruleId)
        if (savedRuleRepresentations) {
            ruleRepresentationCacheableRepository.deleteAll(savedRuleRepresentations)
        }
    }
}