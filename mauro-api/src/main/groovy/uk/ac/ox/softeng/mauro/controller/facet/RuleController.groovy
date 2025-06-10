package uk.ac.ox.softeng.mauro.controller.facet

import uk.ac.ox.softeng.mauro.audit.Audit
import uk.ac.ox.softeng.mauro.web.PaginationParams

import io.micronaut.core.annotation.Nullable
import io.micronaut.http.HttpResponse
import uk.ac.ox.softeng.mauro.api.Paths
import uk.ac.ox.softeng.mauro.api.facet.RuleApi
import uk.ac.ox.softeng.mauro.domain.facet.Rule
import uk.ac.ox.softeng.mauro.domain.facet.RuleRepresentation
import uk.ac.ox.softeng.mauro.domain.model.AdministeredItem
import uk.ac.ox.softeng.mauro.domain.security.Role
import uk.ac.ox.softeng.mauro.persistence.cache.FacetCacheableRepository
import uk.ac.ox.softeng.mauro.persistence.cache.ItemCacheableRepository
import uk.ac.ox.softeng.mauro.persistence.facet.RuleRepresentationRepository
import uk.ac.ox.softeng.mauro.web.ListResponse

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
    @Get(Paths.RULE_LIST_PAGED)
    ListResponse<Rule> list(String domainType, UUID domainId, @Nullable PaginationParams params = new PaginationParams()) {
        AdministeredItem administeredItem = findAdministeredItem(domainType, domainId)
        accessControlService.checkRole(Role.READER, administeredItem)
        ListResponse.from(!administeredItem.rules ? [] : administeredItem.rules, params)
    }

    @Audit
    @Get(Paths.RULE_ID)
    Rule show(@NonNull String domainType, @NonNull UUID domainId, UUID id) {
        Rule rule = super.show(domainType, domainId, id) as Rule
        if (rule) {
            AdministeredItem administeredItem = findAdministeredItem(rule.multiFacetAwareItemDomainType,
                                                                     rule.multiFacetAwareItemId)
            accessControlService.checkRole(Role.READER, administeredItem)
            List<Rule> ruleList = administeredItem.rules
            Rule ruleWithReports = ruleList.find {it -> it.id == id}
            ruleWithReports
        }
    }

    @Audit
    @Post(Paths.RULE_LIST)
    Rule create(@NonNull String domainType, @NonNull UUID domainId, @Body @NonNull Rule rule) {
        super.create(domainType, domainId, rule)
    }

    @Audit
    @Put(Paths.RULE_ID)
    Rule update(@NonNull String domainType, @NonNull UUID domainId, UUID id, @Body @NonNull Rule rule) {
        super.update(id, rule)
    }

    @Audit(deletedObjectDomainType = Rule)
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